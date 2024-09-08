package org.my.quarkus.raft.api.voting;

public record RequestVoteRequest(int term, String candidateId, int lastLogIndex, int lastLogTerm) {
}
