-- KEYS[1] = user_cached:{id}:daily_attempts
-- KEYS[2] = user_cached:{id}:total_attempts

if redis.call("HEXISTS", KEYS[1], "__init__") == 0 then
	redis.call("HSET", KEYS[1], "__init__", "1")
	redis.call("HSET", KEYS[1], "count", 0)
	redis.call("EXPIRE", KEYS[1], 86400)
end

if redis.call("HEXISTS", KEYS[2], "__init__") == 0 then
	redis.call("HSET", KEYS[2], "__init__", "1")
	redis.call("HSET", KEYS[2], "count", 0)
end

return "OK"
