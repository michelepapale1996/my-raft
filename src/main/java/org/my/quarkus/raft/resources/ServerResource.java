package org.my.quarkus.raft.resources;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.my.quarkus.raft.handlers.RequestHandler;
import org.my.quarkus.raft.api.append.entries.AppendEntriesRequest;
import org.my.quarkus.raft.api.append.entries.AppendEntriesResponse;
import org.my.quarkus.raft.api.voting.RequestVoteRequest;
import org.my.quarkus.raft.api.voting.RequestVoteResponse;

@Path("/raft")
public class ServerResource {
    RequestHandler requestHandler = RequestHandler.getInstance();

    @POST
    @Path("/appendEntries")
    public AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest) {
        return requestHandler.appendEntries(appendEntriesRequest);
    }

    @POST
    @Path("/requestVote")
    public RequestVoteResponse requestVote(RequestVoteRequest requestVoteRequest) {
        return requestHandler.requestVote(requestVoteRequest);
    }
}
