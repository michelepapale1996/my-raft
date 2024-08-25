package org.my.quarkus.raft.api;

public record AppendEntriesResponse(long term, boolean success) {
}
