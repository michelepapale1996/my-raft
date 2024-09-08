package org.my.quarkus.raft.api.voting;

public record RequestVoteResponse(int term, boolean voteGranted) {
}
