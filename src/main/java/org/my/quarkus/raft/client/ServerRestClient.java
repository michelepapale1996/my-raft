package org.my.quarkus.raft.client;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.my.quarkus.raft.api.append.entries.AppendEntriesRequest;
import org.my.quarkus.raft.api.append.entries.AppendEntriesResponse;
import org.my.quarkus.raft.api.voting.RequestVoteRequest;
import org.my.quarkus.raft.api.voting.RequestVoteResponse;

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
