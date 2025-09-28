-- KEYS[1] = dailyAttemptsKey
-- KEYS[2] = totalAttemptsKey
-- KEYS[3] = leaderboardKey
-- KEYS[4] = userHashKey
-- ARGV[1] = userId
-- ARGV[2] = score
-- ARGV[3] = maxEventsPerUser
-- ARGV[4] = maxEventsPerUserPerDay
-- ARGV[5] = allowedRegion
-- ARGV[6] = isActive

-- daily attempts
local daily = tonumber(redis.call("GET", KEYS[1]) or "0")
if daily >= tonumber(ARGV[4]) then
	return "No daily attempts left";
end

-- total attempts
local total = tonumber(redis.call("GET", KEYS[2]) or "0")
if total >= tonumber(ARGV[3]) then
	return "No total attempts left";
end

-- check region
local userRegion = redis.call("HGET", KEYS[4], "region")
if not userRegion or userRegion ~= ARGV[5] then
	return "Region mismatch";
end

-- check availability
local active = redis.call("HGET", KEYS[4], "active")
if not active or active ~= ARGV[6] then
	return "User not active";
end

-- update all atomically
redis.call("INCR", KEYS[1])
redis.call("INCR", KEYS[2])
redis.call("ZINCRBY", KEYS[3], tonumber(ARGV[2]), ARGV[1])
redis.call("HINCRBY", KEYS[4], "attempts", 1)

return "success";
