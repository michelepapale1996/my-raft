package org.my.raft.model.api.append.entries;

public record AppendEntriesResponse(int term, boolean success) {
}
