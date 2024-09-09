package org.my.quarkus.client;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.my.raft.api.append.entries.AppendEntriesRequest;
import org.my.raft.api.append.entries.AppendEntriesResponse;
import org.my.raft.api.voting.RequestVoteRequest;
import org.my.raft.api.voting.RequestVoteResponse;

@RegisterRestClient
@Path("/raft")
public interface ServerRestClient {
    @POST
    @Path("/appendEntries")
    AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest);

    @POST
    @Path("/requestVote")
    RequestVoteResponse requestVote(RequestVoteRequest requestVoteRequest);
}
