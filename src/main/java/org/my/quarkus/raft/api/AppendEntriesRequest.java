package org.my.quarkus.raft.api;

public record AppendEntriesRequest(long term, String serverId) {
}
