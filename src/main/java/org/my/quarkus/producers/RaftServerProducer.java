package org.my.quarkus.producers;

import jakarta.enterprise.context.ApplicationScoped;
import org.my.raft.model.cluster.RaftServer;

public class RaftServerProducer {
    @ApplicationScoped
    public RaftServer raftServer() {
        return new RaftServer();
    }
}
