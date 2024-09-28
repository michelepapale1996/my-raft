package org.my.raft.server;

import org.my.raft.model.api.append.entries.AppendEntriesRequest;
import org.my.raft.model.api.append.entries.AppendEntriesResponse;
import org.my.raft.model.api.voting.RequestVoteRequest;
import org.my.raft.model.api.voting.RequestVoteResponse;

public interface ServerClient {
    AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest);

    RequestVoteResponse requestVote(RequestVoteRequest requestVoteRequest);
}
