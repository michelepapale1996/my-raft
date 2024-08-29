package org.my.quarkus.raft.resources;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.my.quarkus.raft.handlers.RequestHandler;
import org.my.quarkus.raft.model.state.machine.StateMachineCommand;

@Path("/objects")
public class ObjectsResource {

    RequestHandler requestHandler = RequestHandler.getInstance();

    @GET
    @Path("/{key}")
    public StateMachineCommand get(String key) {
        return requestHandler.get(key);
    }

    @POST
    public StateMachineCommand set(StateMachineCommand object) {
        requestHandler.set(object);
        return requestHandler.get(object.key());
    }
}
