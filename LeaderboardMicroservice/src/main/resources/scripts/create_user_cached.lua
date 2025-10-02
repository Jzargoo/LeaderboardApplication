-- KEYS[1] = user_cached:{id}
-- KEYS[2] = user_cached:{id}:daily_attempts
-- KEYS[3] = user_cached:{id}:total_attempts
-- ARGV[1] = id
-- ARGV[2] = username
-- ARGV[3] = region

redis.call("HMSET", KEYS[1],
	"id", ARGV[1],
	"username", ARGV[2],
	"region", ARGV[3]
)

redis.call("HSET", KEYS[3], "__init__", "0")

redis.call("HSET", KEYS[2], "__init__", "0")
redis.call("EXPIRE", KEYS[2], 86400)

return {"OK"}
