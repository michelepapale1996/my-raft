package org.my.quarkus.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.my.raft.model.api.append.entries.AppendEntriesRequest;
import org.my.raft.model.api.append.entries.AppendEntriesResponse;
import org.my.raft.model.api.voting.RequestVoteRequest;
import org.my.raft.model.api.voting.RequestVoteResponse;
import org.my.raft.server.RaftServer;
import org.my.raft.model.ServerState;

// This class is not a REST endpoint, but for the sake of simplicity, we will use it as one
@Path("/raft")
public class ServerResource {

    @Inject
    RaftServer raftServer;

    @POST
    @Path("/appendEntries")
    public AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest) {
        return raftServer.acceptAppendEntries(appendEntriesRequest);
    }

    @POST
    @Path("/requestVote")
    public RequestVoteResponse requestVote(RequestVoteRequest requestVoteRequest) {
        return raftServer.requestVote(requestVoteRequest);
    }

    @GET
    @Path("/state")
    public ServerState getState() {
        return raftServer.getServerState();
    }
}
