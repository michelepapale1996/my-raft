# Raft implementation in Quarkus

This is a simple implementation of the Raft consensus algorithm in Quarkus. It is based on the [Raft paper](https://raft.github.io/raft.pdf).

## Running the application
Open multiple terminals and run the following command in each terminal:
```shell
# Node running on port 9092
export cluster_hosts=http://localhost:9093,http://localhost:9094; export quarkus_http_port=9092; ./mvnw quarkus:dev

# Node running on port 9093
export cluster_hosts=http://localhost:9092,http://localhost:9094; export quarkus_http_port=9093; ./mvnw quarkus:dev

# Node running on port 9094
export cluster_hosts=http://localhost:9092,http://localhost:9093; export quarkus_http_port=9094; ./mvnw quarkus:dev
```

## Endpoints
- `GET /raft/state`: Get the current status of the node
- `GET /objects/{:id}`: Retrieve a value from the Raft cluster. {:id} is the key of the object you want to retrieve
- `POST /objects`: Set a value in the Raft cluster. The body must be a JSON object with the following structure:
```json
{
  "key": "1",
  "value": "value_for_1"
}
```

Following you can find examples of curls to test the application (assuming that the leader is running on port 9092):
```shell
# Get the status of the node
curl -X GET http://localhost:9092/raft/state 

# Set a value in the Raft cluster
curl -X POST http://localhost:9092/objects -H "Content-Type: application/json" -d '{"key": "1", "value": "value_for_1"}'

# Retrieve a value from the Raft cluster
curl -X GET http://localhost:9092/objects/1
```



## The project in action
After you run the three nodes, you should see that one the nodes is elected as the leader. You can check the status of the nodes by issuing a GET request to the `/raft/state` endpoint. The leader node should have the `LEADER` status.

## Open points
- [X] Add unit tests to Log
- [ ] Add unit and integration tests on RaftServer
- [ ] Ensure consistency and isolation of the algorithm. An idea could be to make the RaftServer state immutable for better concurrency control
- [ ] Whenever a client issues a command (also for get requests), validation must be performed to ensure that the node is the leader
- [ ] The client set request is synchronous to the heartbeating mechanism
- [ ] Every time an illegal state is reached, the node should log it's current state
- [ ] Up to now the log replication is working but the election restriction at 5.4.1 is not implemented yet. This leads to the fact that the leader can be elected even if it has not the most up-to-date log
- [ ] Log must be persisted to disk, up to now is only in memory