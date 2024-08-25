package org.my.quarkus.raft.api;

public record RequestVoteRequest(long term, String candidateId, long lastLogIndex, long lastLogTerm) {
}
