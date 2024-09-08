package org.my.quarkus.raft.handlers;

import org.my.quarkus.raft.api.append.entries.AppendEntriesRequest;
import org.my.quarkus.raft.api.append.entries.AppendEntriesResponse;
import org.my.quarkus.raft.api.voting.RequestVoteRequest;
import org.my.quarkus.raft.api.voting.RequestVoteResponse;
import org.my.quarkus.raft.model.cluster.RaftServer;
import org.my.quarkus.raft.model.log.LogEntry;
import org.my.quarkus.raft.model.state.machine.StateMachineCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public enum RequestHandler {
    INSTANCE;

    public static RequestHandler getInstance() {
        return RequestHandler.INSTANCE;
    }

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    private final RaftServer raftServer = RaftServer.getInstance();

    public AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest) {
        logger.info(appendEntriesRequest.toString());
        return raftServer.accept(appendEntriesRequest);
    }

    public RequestVoteResponse requestVote(RequestVoteRequest requestVoteRequest) {
        logger.info(requestVoteRequest.toString());

        if (requestVoteRequest.term() < raftServer.getCurrentTerm()) {
            return new RequestVoteResponse(raftServer.getCurrentTerm(), false);
        }

        Optional<String> votedForOptional = raftServer.getVotedFor();
        Optional<LogEntry> logEntry = raftServer.getLog().get(requestVoteRequest.lastLogIndex());
        if (
                (votedForOptional.isEmpty() || votedForOptional.get().equals(requestVoteRequest.candidateId())) &&
                        (logEntry.isEmpty() || logEntry.get().term() == requestVoteRequest.lastLogTerm())) {
            raftServer.switchToFollower();
            raftServer.setCurrentTerm(requestVoteRequest.term());
            raftServer.setVotedFor(requestVoteRequest.candidateId());
            return new RequestVoteResponse(requestVoteRequest.term(), true);
        }
        return new RequestVoteResponse(raftServer.getCurrentTerm(), false);
    }

    public StateMachineCommand get(String key) {
        return new StateMachineCommand(key, raftServer.getStateMachine().get(key));
    }

    public void set(StateMachineCommand object) {
        logger.info("Received request: {}", object.toString());
        raftServer.set(object.key(), object.value());
    }
}
