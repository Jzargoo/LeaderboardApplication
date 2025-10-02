-- KEYS[1] = leaderboard:{id}:mutable/immutable
-- KEYS[2] = leaderboard_information:{id}
-- ARGV[1] = ownerId
-- ARGV[2] = initialValue
-- ARGV[3] = id
-- ARGV[4] = description
-- ARGV[5] = isPublic
-- ARGV[6] = isMutable
-- ARGV[7] = isShowTies
-- ARGV[8] = globalRange
-- ARGV[9] = createdAt
-- ARGV[10] = expireAt
-- ARGV[11] = maxScore
-- ARGV[12] = regions
-- ARGV[13] = maxEventsPerUser
-- ARGV[14] = maxEventsPerUserPerDay

redis.call("ZADD", KEYS[1], ARGV[2], ARGV[1])

redis.call("HMSET", KEYS[2],
	"id", ARGV[3],
	"description", ARGV[4],
	"ownerId", ARGV[1],
	"initialValue", ARGV[2],
	"isPublic", ARGV[5],
	"isMutable", ARGV[6],
	"showTies", ARGV[7],
	"globalRange", ARGV[8],
	"createdAt", ARGV[9],
	"expireAt", ARGV[10],
	"regions", ARGV[12],
	"maxEventsPerUser", ARGV[13],
	"maxEventsPerUserPerDay", ARGV[14],
	"maxScore", ARGV[11]
)

return {"OK"}
