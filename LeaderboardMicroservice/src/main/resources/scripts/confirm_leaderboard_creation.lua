-- KEY[1] = leaderbaord_information:{id}
-- KEY[2] = leaderboard_signal:{id}

-- ARGV[1] = ttl

redis.call("HSET", KEY[1], "isActive", "true")
redis.call("SET", KEYS[2], "signal", "PX", ARGV[1])

return "success"