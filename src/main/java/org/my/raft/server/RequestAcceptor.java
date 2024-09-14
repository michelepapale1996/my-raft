package org.my.raft.server;

import jakarta.ws.rs.NotFoundException;
import org.my.raft.model.api.append.entries.AppendEntriesRequest;
import org.my.raft.model.api.append.entries.AppendEntriesResponse;
import org.my.raft.model.api.voting.RequestVoteRequest;
import org.my.raft.model.api.voting.RequestVoteResponse;
import org.my.raft.model.log.LogEntry;
import org.my.raft.model.state.machine.StateMachineCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class RequestAcceptor {
    private static final Logger logger = LoggerFactory.getLogger(RequestAcceptor.class);

    private final RaftServer raftServer;

    public RequestAcceptor(RaftServer raftServer) {
        this.raftServer = raftServer;
    }


    public AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest) {
        logger.info(appendEntriesRequest.toString());
        return raftServer.acceptAppendEntries(appendEntriesRequest);
    }

    public RequestVoteResponse requestVote(RequestVoteRequest requestVoteRequest) {
        logger.info(requestVoteRequest.toString());

        if (requestVoteRequest.term() < raftServer.getCurrentTerm()) {
            return new RequestVoteResponse(raftServer.getCurrentTerm(), false);
        }

        Optional<String> votedForOptional = raftServer.getVotedFor();
        Optional<LogEntry> logEntry = raftServer.getLog().entryAt(requestVoteRequest.lastLogIndex());
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
        Optional<String> optionalValue = raftServer.getStateMachine().get(key);
        if (optionalValue.isEmpty()) {
            throw new NotFoundException();
        }
        return new StateMachineCommand(key, optionalValue.get());
    }

    public void set(StateMachineCommand object) {
        logger.info("Received request: {}", object.toString());
        raftServer.set(object.key(), object.value());
    }
}
