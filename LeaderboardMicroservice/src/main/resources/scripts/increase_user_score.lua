-- KEYS[1] = dailyAttemptsKey
-- KEYS[2] = totalAttemptsKey
-- KEYS[3] = leaderboardKey
-- KEYS[4] = userHashKey
-- KEYS[5] = pubsubChannel
-- KEYS[6] = countGloablTop
-- ARGV[1] = userId
-- ARGV[2] = scoreDelta
-- ARGV[3] = maxEventsPerUser
-- ARGV[4] = maxEventsPerUserPerDay
-- ARGV[6] = allowedRegion
-- ARGV[6] = isActive
-- ARGV[7] = uuid

-- daily attempts
local daily = tonumber(redis.call("GET", KEYS[1]) or "0")
if daily >= tonumber(ARGV[4]) then
  return "No daily attempts left"
end

-- total attempts
local total = tonumber(redis.call("GET", KEYS[2]) or "0")
if total >= tonumber(ARGV[3]) then
  return "No total attempts left"
end

-- check region
local userRegion = redis.call("HGET", KEYS[4], "region")
if userRegion ~= ARGV[5] then
  return "Region mismatch"
end

-- check availability
local active = redis.call("HGET", KEYS[6], "active")
if not active or active ~= "1" then
  return "User not active"
end


local cjson = require "cjson"

local range_result = redis.call('ZRANGE', KEYS[3], 0, KEYS[6] - 1, 'WITHSCORES')

local leaderboard = {}
for i=1,#range_result,2 do
  table.insert(leaderboard, {
    userId = range_result[i],
    score  = tonumber(range_result[i+1])
  })
end

local payload = cjson.encode({
  lbId = KEYS[3],
  top  = leaderboard
})

-- update all atomically
redis.call("INCR", KEYS[1])
redis.call("INCR", KEYS[2])
redis.call("PUBLISH",pubsubChannel ,payload)
redis.call("ZINCRBY", KEYS[3], ARGV[2], ARGV[1])
redis.call("HINCRBY", KEYS[4], "attempts", 1)

return "success"
