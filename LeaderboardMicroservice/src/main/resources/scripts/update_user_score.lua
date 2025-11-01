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
-- ARGV[6] = userRegion
-- ARGV[7] = countGlobalTop
-- ARGV[8] = lbId

-- Daily attempts
local daily = tonumber(redis.call("HGET", KEYS[1], "__init__") or "0")
if daily >= tonumber(ARGV[4]) then
	return "No daily attempts left"
end

-- Total attempts
local total = tonumber(redis.call("HGET", KEYS[2], "__init__") or "0")
if total >= tonumber(ARGV[3]) then
	return "No total attempts left"
end

-- Region check
local leaderboardRegion = ARGV[5]
local userRegion = ARGV[6]

if leaderboardRegion ~= "ZZ" and not string.find(leaderboardRegion, userRegion, 1, true) then
	return "Region mismatch"
end

-- Active status
local active = redis.call("HGET", KEYS[4], "active")
if active ~= "true" then
	return "User not active"
end

-- Old rank
local oldRank = redis.call("ZREVRANK", KEYS[3], ARGV[1])

-- Atomic updates
redis.call("HINCRBY", KEYS[1], "__init__", 1)
redis.call("HINCRBY", KEYS[2], "__init__", 1)
redis.call("HINCRBY", KEYS[4], "attempts", 1)
redis.call("ZINCRBY", KEYS[3], tonumber(ARGV[2]), ARGV[1])

-- Build Top N
local topCount = tonumber(ARGV[7])
local range_result = redis.call("ZREVRANGE", KEYS[3], 0, topCount - 1, "WITHSCORES")

local leaderboard = {}
for i = 1, #range_result, 2 do
	table.insert(leaderboard, {
		userId = range_result[i],
		score  = tonumber(range_result[i+1])
	})
end

-- JSON payload for global stream
local payload = cjson.encode({
	lbKey = KEYS[3],
	lbId = ARGV[8],
	maxTop = topCount,
	top = leaderboard
})

-- Local stream event
redis.call("XADD", KEYS[6], "*",
	"oldRank", oldRank or -1,
	"userId", ARGV[1],
	"leaderboardKey", KEYS[3],
	"lbId", ARGV[8]
)

-- Global stream event (only when oldRank < topCount)
if oldRank ~= nil and tonumber(oldRank) < topCount then
	redis.call("XADD", KEYS[5], "*", "payload", payload)
end

return "success"
