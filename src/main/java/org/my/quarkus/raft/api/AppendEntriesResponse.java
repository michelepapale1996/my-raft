package org.my.quarkus.raft.api;

public record AppendEntriesResponse(int term, boolean success) {
}
