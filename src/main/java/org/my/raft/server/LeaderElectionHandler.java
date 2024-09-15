package org.my.raft.server;

import org.my.raft.model.api.voting.RequestVoteRequest;
import org.my.raft.model.api.voting.RequestVoteResponse;
import org.my.raft.model.log.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class LeaderElectionHandler {

    private final RaftServer raftServer;
    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionHandler.class);

    public LeaderElectionHandler(RaftServer raftServer) {
        this.raftServer = raftServer;
    }

    void triggerElection() {
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
        int lastLogIndex = 0;
        int lastLogTerm = 0;
        if (raftServer.getLog().size() > 0) {
            LogEntry logEntry = raftServer.getLog().entryAt(raftServer.getLog().size() - 1);
            lastLogIndex = logEntry.index();
            lastLogTerm = logEntry.term();
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
