storage "raft" {
  path = "/vault/raft"
  node_id = "raft_node_id"
}

listener "tcp" {
  address     = "0.0.0.0:8200"

  }
api_addr = "https://127.0.0.1:8200"
ui = true

disable_mlock = true