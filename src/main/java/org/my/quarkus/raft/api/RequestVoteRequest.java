package org.my.quarkus.raft.api;

public record RequestVoteRequest(int term, String candidateId, int lastLogIndex, int lastLogTerm) {
}
