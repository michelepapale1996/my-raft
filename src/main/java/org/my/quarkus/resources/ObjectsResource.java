package org.my.quarkus.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.my.raft.model.cluster.RaftServer;
import org.my.raft.model.state.machine.StateMachineCommand;

@Path("/objects")
public class ObjectsResource {

    @Inject
    RaftServer raftServer;

    @GET
    @Path("/{key}")
    public StateMachineCommand get(String key) {
        return raftServer.getRequestHandler().get(key);
    }

    @POST
    public StateMachineCommand set(StateMachineCommand object) {
        raftServer.getRequestHandler().set(object);
        return raftServer.getRequestHandler().get(object.key());
    }
}
