-- KEYS[1] = dailyAttemptsKey
-- KEYS[2] = totalAttemptsKey
-- KEYS[3] = leaderboardKey
-- KEYS[4] = userHashKey
-- KEYS[5] = StreamGlobal
-- KEYS[6] = StreamLocal

-- ARGV[1] = userId
-- ARGV[2] = scoreDelta
-- ARGV[3] = maxEventsPerUser
-- ARGV[4] = maxEventsPerUserPerDay
-- ARGV[5] = allowedRegion
-- ARGV[6] = countGlobalTop
-- ARGV[7] = lbId

-- daily attempts
local daily = tonumber(redis.call("HGET", KEYS[1], "__init__") or "0")


if daily >= tonumber(ARGV[4]) then
  return "No daily attempts left"
end

-- total attempts
local total = tonumber(redis.call("HGET", KEYS[2], "__init__") or "0")
if total >= tonumber(ARGV[3]) then
  return "No total attempts left"
end

-- check region
local userRegion = redis.call("HGET", KEYS[4], "region")
if userRegion ~= ARGV[5] then
  return "Region mismatch"
end

-- check availability
local active = redis.call("HGET", KEYS[4], "active")
if active ~= "true" then
  return "User not active"
end

-- old rank before update
local oldRank = redis.call("ZRANK", KEYS[3], ARGV[1])

-- update counters and score
redis.call("INCR", KEYS[1]) -- daily attempts
redis.call("INCR", KEYS[2]) -- total attempts
redis.call("HINCRBY", KEYS[4], "attempts", 1)
redis.call("ZINCRBY", KEYS[3], tonumber(ARGV[2]), ARGV[1])

-- build updated leaderboard
local range_result = redis.call('ZRANGE', KEYS[3], 0, tonumber(ARGV[6]) - 1, 'WITHSCORES')

local leaderboard = {}
for i=1,#range_result,2 do
  table.insert(leaderboard, {
    userId = range_result[i],
    score  = tonumber(range_result[i+1])
  })
end

local payload = cjson.encode({
  lbKey = KEYS[3],
  lbId = ARGV[7],
  maxTop = tonumber(ARGV[6])
})

local oldRankValue = ARGV[3] or -1

-- publish to streams
redis.call("XADD", KEYS[5], "*", "payload", payload)
redis.call("XADD", KEYS[6], "*", "oldRank", oldRankValue)

return "success"
