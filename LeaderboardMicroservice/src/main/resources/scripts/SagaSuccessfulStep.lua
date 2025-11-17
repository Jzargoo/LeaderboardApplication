local sagaKey = KEYS[1]

local leaderboardId = ARGV[1]
local newStatus = ARGV[2]
local lastCompletedStep = ARGV[3]

local currentStep = redis.call("HGET", sagaKey, "lastStepCompleted")

if currentStep ~= lastCompletedStep then
	return {err = "STATE_MISMATCH: expected " .. expectedCurrentStep .. " but was " .. tostring(currentStep)}
end

redis.call("HSET", sagaKey, "status", newStatus)
redis.call("HSET", sagaKey, "leaderboardId", leaderboardId)
redis.call("HSET", sagaKey, "lastStepCompleted", lastCompletedStep)

return "OK"
