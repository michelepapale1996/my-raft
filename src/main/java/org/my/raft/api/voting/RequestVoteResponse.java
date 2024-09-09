package org.my.raft.api.voting;

public record RequestVoteResponse(int term, boolean voteGranted) {
}
