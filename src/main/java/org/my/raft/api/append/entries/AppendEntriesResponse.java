package org.my.raft.api.append.entries;

public record AppendEntriesResponse(int term, boolean success) {
}
