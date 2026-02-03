-- KEYS[1] = leaderboard:{id}:mutable/immutable
-- KEYS[2] = leaderboard_information:{id}
-- KEYS[3] = leaderobard_signal:{id}
-- KEYS[4] = saga_controlling_state:{sagaId}

redis.call("DEL", KEYS[1])
redis.call("DEL", KEYS[2])
redis.call("DEL", KEYS[3])
redis.call("DEL", KEYS[4])
return "OK"
