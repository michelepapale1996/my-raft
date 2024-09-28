package org.my.raft.model.api.voting;

public record RequestVoteResponse(int term, boolean voteGranted) {
}
