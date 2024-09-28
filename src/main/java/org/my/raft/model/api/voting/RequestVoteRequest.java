package org.my.raft.model.api.voting;

public record RequestVoteRequest(int term, String candidateId, int lastLogIndex, int lastLogTerm) {
}
