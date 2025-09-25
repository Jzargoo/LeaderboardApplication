-- KEYS[1] = dailyAttemptsKey
-- KEYS[2] = totalAttemptsKey
-- KEYS[3] = leaderboardKey
-- KEYS[4] = userHashKey
-- ARGV[1] = userId
-- ARGV[2] = scoreDelta
-- ARGV[3] = maxEventsPerUser
-- ARGV[4] = maxEventsPerUserPerDay

-- daily attempts
local daily = tonumber(redis.call("GET", KEYS[1]) or "0")
if daily >= tonumber(ARGV[4]) then
  return {err="No daily attempts left"}
end

-- total attempts
local total = tonumber(redis.call("GET", KEYS[2]) or "0")
if total >= tonumber(ARGV[3]) then
  return {err="No total attempts left"}
end

-- update all atomically
redis.call("INCR", KEYS[1])
redis.call("INCR", KEYS[2])
redis.call("ZINCRBY", KEYS[3], ARGV[2], ARGV[1])
redis.call("HINCRBY", KEYS[4], "attempts", 1)

return {ok="success"}
