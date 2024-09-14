package org.my.raft.server;

import org.my.raft.model.api.voting.RequestVoteRequest;
import org.my.raft.model.api.voting.RequestVoteResponse;
import org.my.raft.model.log.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class LeaderElectionHandler implements Runnable {

    private final RaftServer raftServer;
    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionHandler.class);

    public LeaderElectionHandler(RaftServer raftServer) {
        this.raftServer = raftServer;
    }

    @Override
    public void run() {
        try {
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
        } catch (Exception e) {
            logger.error("Error while running leader election handler", e);
        }
    }

    private void triggerElection() {
        this.raftServer.switchToCandidate();

        logger.info("Starting election for term {}", this.raftServer.getCurrentTerm());

        RequestVoteRequest requestVoteRequest = buildRequestVoteRequest(this.raftServer);

        List<RequestVoteResponse> responses = this.raftServer.getRequestExecutor().performVoteRequest(requestVoteRequest);

        logger.info("Received {} responses out of {} nodes during the election process",
                responses.size(),
                this.raftServer.getClusterState().getClusterSize() - 1);

        if (gainedQuorum(this.raftServer.getClusterState().getClusterSize(), responses)) {
            logger.info("I'm the leader! Node with uuid: {}", raftServer.getUuid());
            this.raftServer.switchToLeader();
        } else {
            logger.info("I'm not the leader since I've not gained the quorum!");
        }
    }

    private boolean gainedQuorum(int numberOfServersInCluster, List<RequestVoteResponse> responses) {
        return responses.stream().filter(RequestVoteResponse::voteGranted).count() > numberOfServersInCluster / 2;
    }

    private RequestVoteRequest buildRequestVoteRequest(RaftServer raftServer) {
        Optional<LogEntry> lastLogEntry = raftServer.getLog().lastLogEntry();
        int lastLogIndex = 0;
        int lastLogTerm = 0;
        if (lastLogEntry.isPresent()) {
            lastLogIndex = lastLogEntry.get().index();
            lastLogTerm = lastLogEntry.get().term();
        }
        RequestVoteRequest requestVoteRequest = new RequestVoteRequest(
                raftServer.getCurrentTerm(),
                raftServer.getUuid(),
                lastLogIndex,
                lastLogTerm
        );
        return requestVoteRequest;
    }
}
