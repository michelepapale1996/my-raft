package org.my.raft.model.api.append.entries;

import org.my.raft.model.log.LogEntry;

import java.util.List;

public record AppendEntriesRequest(int term,
                                   String serverId,
                                   int prevLogIndex,
                                   int prevLogTerm,
                                   List<LogEntry> entries,
                                   int leaderCommit) {

}
