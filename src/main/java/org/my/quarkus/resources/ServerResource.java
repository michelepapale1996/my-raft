package org.my.quarkus.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.my.raft.handlers.RequestHandler;
import org.my.raft.api.append.entries.AppendEntriesRequest;
import org.my.raft.api.append.entries.AppendEntriesResponse;
import org.my.raft.api.voting.RequestVoteRequest;
import org.my.raft.api.voting.RequestVoteResponse;
import org.my.raft.model.cluster.RaftServer;
import org.my.raft.model.cluster.ServerState;

// This class is not a REST endpoint, but for the sake of simplicity, we will use it as one
@Path("/raft")
public class ServerResource {

    @Inject
    RaftServer raftServer;

    @POST
    @Path("/appendEntries")
    public AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest) {
        return raftServer.getRequestHandler().appendEntries(appendEntriesRequest);
    }

    @POST
    @Path("/requestVote")
    public RequestVoteResponse requestVote(RequestVoteRequest requestVoteRequest) {
        return raftServer.getRequestHandler().requestVote(requestVoteRequest);
    }

    @GET
    @Path("/state")
    public ServerState getState() {
        return raftServer.getServerState();
    }
}
