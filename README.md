# Raft implementation in Quarkus

This is a simple implementation of the Raft consensus algorithm in Quarkus. It is based on the [Raft paper](https://raft.github.io/raft.pdf).

## Running the application
Open multiple terminals and run the following command in each terminal:
```shell
# Node running on port 9092
export JAVA_HOME=/Users/michele.papale/.asdf/installs/java/openjdk-17; export cluster_hosts=http://localhost:9093,http://localhost:9094; export quarkus_http_port=9092; ./mvnw quarkus:dev

# Node running on port 9093
export JAVA_HOME=/Users/michele.papale/.asdf/installs/java/openjdk-17; export cluster_hosts=http://localhost:9092,http://localhost:9094; export quarkus_http_port=9093; ./mvnw quarkus:dev

# Node running on port 9094
export JAVA_HOME=/Users/michele.papale/.asdf/installs/java/openjdk-17; export cluster_hosts=http://localhost:9092,http://localhost:9093; export quarkus_http_port=9094; ./mvnw quarkus:dev
```

## Open points
- [X] Add unit tests to Log
- [ ] Add unit and integration tests on RaftServer
- [ ] Ensure consistency and isolation of the algorithm. An idea could be to make the RaftServer state immutable for better concurrency control
- [ ] Whenever a client issues a command (also for get requests), validation must be performed to ensure that the node is the leader
- [ ] The client set request is synchronous to the heartbeating mechanism
- [ ] Every time an illegal state is reached, the node should log it's current state
- [ ] Up to now the log replication is working but the election restriction at 5.4.1 is not implemented yet. This leads to the fact that the leader can be elected even if it has not the most up-to-date log