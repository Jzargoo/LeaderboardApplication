-- KEYS[1] = dailyAttemptsKey
-- KEYS[2] = totalAttemptsKey
-- KEYS[3] = leaderboardKey
-- KEYS[4] = userHashKey
-- KEYS[5] = pubsubChannelGlobal
-- KEYS[6] = pubsubChannelLocal

-- ARGV[1] = userId
-- ARGV[2] = scoreDelta
-- ARGV[3] = maxEventsPerUser
-- ARGV[4] = maxEventsPerUserPerDay
-- ARGV[5] = allowedRegion
-- ARGV[6] = countGlobalTop

-- check daily attempts
local daily = tonumber(redis.call("GET", KEYS[1]) or "0")
if daily >= tonumber(ARGV[4]) then
	return "No daily attempts left"
end

-- check total attempts
local total = tonumber(redis.call("GET", KEYS[2]) or "0")
if total >= tonumber(ARGV[3]) then
	return "No total attempts left"
end

-- check region
local userRegion = redis.call("HGET", KEYS[4], "region")
if not userRegion or userRegion ~= ARGV[5] then
	return "Region mismatch"
end

-- check activity status
local active = redis.call("HGET", KEYS[4], "active")
if active ~= "true" then
	return "User not active"
end

-- get old rank before update
local oldRank = redis.call("ZREVRANK", KEYS[3], ARGV[1])

-- update attempts and score atomically
redis.call("INCR", KEYS[1])
redis.call("INCR", KEYS[2])
redis.call("ZINCRBY", KEYS[3], tonumber(ARGV[2]), ARGV[1])
redis.call("HINCRBY", KEYS[4], "attempts", 1)

-- fetch updated leaderboard top N
local range_result = redis.call("ZREVRANGE", KEYS[3], 0, tonumber(ARGV[6]) - 1, "WITHSCORES")

local leaderboard = {}
for i=1,#range_result,2 do
	table.insert(leaderboard, {
		userId = range_result[i],
		score  = tonumber(range_result[i+1])
	})
end

-- encode leaderboard as JSON
local payload = cjson.encode({
	lbId = KEYS[3],
	top  = leaderboard
})

-- publish leaderboard and old rank
redis.call("PUBLISH", KEYS[5], payload)
redis.call("PUBLISH", KEYS[6], oldRank or -1)

return "success"
