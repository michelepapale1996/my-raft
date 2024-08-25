package org.my.quarkus.raft.handlers;

import org.my.quarkus.raft.api.RequestVoteRequest;
import org.my.quarkus.raft.api.RequestVoteResponse;
import org.my.quarkus.raft.model.RaftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class LeaderElectionHandler implements Runnable {

    private final RaftServer raftServer = RaftServer.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionHandler.class);

    @Override
    public void run() {
        // if I'm the leader, I don't need to trigger an election
        if (this.raftServer.isLeader()) {
            logger.info("I'm the leader, no need to trigger an election");
            return;
        }

        if (!raftServer.hasReceivedHeartbeat()) {
            logger.info("Starting election since I've not received the heartbeat...");
            triggerElection();
        }
        raftServer.resetReceivedHeartbeat();
    }

    private void triggerElection() {
        this.raftServer.switchToCandidate();

        logger.info("Starting election for term {}", this.raftServer.getCurrentTerm());

        RequestVoteRequest requestVoteRequest = new RequestVoteRequest(
                this.raftServer.getCurrentTerm(),
                this.raftServer.getUuid(),
                -1,
                -1);

        // todo: send request in parallel
        List<RequestVoteResponse> responses = this.raftServer.getClusterState().getServerRestClients().stream()
                .map(serverRestClient -> {
                    try {
                        return serverRestClient.requestVote(requestVoteRequest);
                    } catch (Exception e) {
                        logger.error("Error while sending request vote");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        logger.info("Received {} responses out of {} nodes during the election process",
                responses.size(),
                this.raftServer.getClusterState().getServerRestClients().size());

        int numberOfServersInCluster = this.raftServer.getClusterState().getClusterSize();
        if (responses.stream().filter(RequestVoteResponse::voteGranted).count() > numberOfServersInCluster / 2) {
            logger.info("I'm the leader! Node with uuid: {}", raftServer.getUuid());
            this.raftServer.switchToLeader();
        } else {
            logger.info("I'm not the leader since I've not gained the quorum!");
        }
    }
}
