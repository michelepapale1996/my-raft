package org.my.quarkus.raft.api;

public record RequestVoteResponse(int term, boolean voteGranted) {
}
