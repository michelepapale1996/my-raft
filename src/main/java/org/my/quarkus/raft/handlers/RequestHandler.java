package org.my.quarkus.raft.handlers;

import org.my.quarkus.raft.api.AppendEntriesRequest;
import org.my.quarkus.raft.api.AppendEntriesResponse;
import org.my.quarkus.raft.api.RequestVoteRequest;
import org.my.quarkus.raft.api.RequestVoteResponse;
import org.my.quarkus.raft.model.RaftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum RequestHandler {
    INSTANCE;

    public static RequestHandler getInstance() {
        return RequestHandler.INSTANCE;
    }

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    private final RaftServer raftServer = RaftServer.getInstance();

    public AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest) {
        logger.info(appendEntriesRequest.toString());

        if (raftServer.isLeader()) {
            logger.error("Received append entries request from another leader. Current leader is {}", raftServer.getUuid());
            return new AppendEntriesResponse(raftServer.getCurrentTerm(), false);
        }

        if (appendEntriesRequest.term() < raftServer.getCurrentTerm()) {
            return new AppendEntriesResponse(raftServer.getCurrentTerm(), false);
        } else {
            raftServer.switchToFollower(); // in this way, if I was a candidate I will become a follower
            raftServer.setCurrentTerm(appendEntriesRequest.term());
            raftServer.setVotedFor(appendEntriesRequest.serverId());

            raftServer.setReceivedHeartbeat();

            return new AppendEntriesResponse(appendEntriesRequest.term(), true);
        }
    }

    public RequestVoteResponse requestVote(RequestVoteRequest requestVoteRequest) {
        logger.info(requestVoteRequest.toString());

        if (requestVoteRequest.term() < raftServer.getCurrentTerm()) {
            return new RequestVoteResponse(raftServer.getCurrentTerm(), false);
        }

        if (raftServer.getVotedFor().isEmpty() || raftServer.getVotedFor().get().equals(requestVoteRequest.candidateId())) {
            raftServer.switchToFollower();
            raftServer.setCurrentTerm(requestVoteRequest.term());
            raftServer.setVotedFor(requestVoteRequest.candidateId());
            return new RequestVoteResponse(requestVoteRequest.term(), true);
        }
        return new RequestVoteResponse(raftServer.getCurrentTerm(), false);
    }
}
