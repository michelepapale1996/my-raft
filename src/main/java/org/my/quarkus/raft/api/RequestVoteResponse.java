package org.my.quarkus.raft.api;

public record RequestVoteResponse(long term, boolean voteGranted) {
}
