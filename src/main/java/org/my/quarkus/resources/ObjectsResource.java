package org.my.quarkus.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.my.raft.server.RaftServer;
import org.my.raft.model.state.machine.StateMachineCommand;

@Path("/objects")
public class ObjectsResource {

    @Inject
    RaftServer raftServer;

    @GET
    @Path("/{key}")
    public StateMachineCommand get(String key) {
        return raftServer.getRequestAcceptor().get(key);
    }

    @POST
    public StateMachineCommand set(StateMachineCommand object) {
        raftServer.getRequestAcceptor().set(object);
        return raftServer.getRequestAcceptor().get(object.key());
    }
}
