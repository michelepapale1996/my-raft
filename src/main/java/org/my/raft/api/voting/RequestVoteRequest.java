package org.my.raft.api.voting;

public record RequestVoteRequest(int term, String candidateId, int lastLogIndex, int lastLogTerm) {
}
