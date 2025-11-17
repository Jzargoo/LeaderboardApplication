-- KEYS[1] = leaderboard:{id}:mutable/immutable
-- KEYS[2] = leaderboard_information:{id}
-- KEYS[3] = leaderobard_signal:{id}

redis.call("DEL", KEYS[1])
redis.call("DEL", KEYS[2])
redis.call("DEL", KEYS[3])

return "OK"
