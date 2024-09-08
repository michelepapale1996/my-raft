package org.my.quarkus.raft.api.append.entries;

public record AppendEntriesResponse(int term, boolean success) {
}
