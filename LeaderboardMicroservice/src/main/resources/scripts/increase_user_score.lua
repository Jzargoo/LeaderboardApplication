-- KEYS:
-- 1 = dailyAttemptsKey
-- 2 = totalAttemptsKey
-- 3 = leaderboardKey
-- 4 = userHashKey
-- 5 = StreamGlobal
-- 6 = StreamLocal

-- ARGV:
-- 1 = userId
-- 2 = scoreDelta
-- 3 = maxEventsPerUser
-- 4 = maxEventsPerUserPerDay
-- 5 = topCount
-- 6 = lbId

local userId = ARGV[1]
local scoreDelta = tonumber(ARGV[2])
local maxTotal = tonumber(ARGV[3])
local maxDaily = tonumber(ARGV[4])
local topCount = tonumber(ARGV[5])
local lbId = ARGV[6]

-- daily attempts
local daily = tonumber(redis.call("HGET", KEYS[1], "__init__") or "0")
if daily >= maxDaily then
  return "No daily attempts left"
end

-- total attempts
local total = tonumber(redis.call("HGET", KEYS[2], "__init__") or "0")
if total >= maxTotal then
  return "No total attempts left"
end

-- check active
local active = redis.call("HGET", KEYS[4], "active")
if active ~= "true" then
  return "User not active"
end

-- old rank before update
local oldRank = tonumber(redis.call("ZREVRANK", KEYS[3], userId)) or -1

-- update counters and score
redis.call("HINCRBY", KEYS[1], "__init__", 1)
redis.call("HINCRBY", KEYS[2], "__init__", 1)
redis.call("HINCRBY", KEYS[4], "attempts", 1)
redis.call("ZINCRBY", KEYS[3], scoreDelta, userId)

-- new rank after update
local newRank = tonumber(redis.call("ZREVRANK", KEYS[3], userId)) or -1

-- determine if we need to publish to global stream
local publishGlobal = (oldRank >= 0 and oldRank < topCount) or (newRank >= 0 and newRank < topCount)

-- local stream always
redis.call("XADD", KEYS[6], "*",
    "oldRank", tostring(oldRank),
    "newRank", tostring(newRank),
    "userId", userId,
    "leaderboardKey", KEYS[3],
    "lbId", lbId
)

-- global stream only if top affected
if publishGlobal then

    local range_result = redis.call('ZREVRANGE', KEYS[3], 0, topCount - 1, 'WITHSCORES')
    local leaderboard = {}
    for i = 1, #range_result, 2 do
        table.insert(leaderboard, {
            userId = range_result[i],
            score = tonumber(range_result[i+1])
        })
    end

    local payload = cjson.encode({
        lbKey = KEYS[3],
        lbId = lbId,
        maxTop = topCount,
        top = leaderboard
    })

    redis.call("XADD", KEYS[5], "*", "payload", payload)
end

return "success"
