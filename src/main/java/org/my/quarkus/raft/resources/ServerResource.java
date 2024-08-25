package org.my.quarkus.raft.resources;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.my.quarkus.raft.handlers.RequestHandler;
import org.my.quarkus.raft.api.AppendEntriesRequest;
import org.my.quarkus.raft.api.AppendEntriesResponse;
import org.my.quarkus.raft.api.RequestVoteRequest;
import org.my.quarkus.raft.api.RequestVoteResponse;

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
