package org.my.quarkus.raft.startup;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.my.quarkus.raft.handlers.Scheduler;
import org.my.quarkus.raft.model.ClusterState;
import org.my.quarkus.raft.model.RaftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@ApplicationScoped
public class ServerBootstrapper {

    private static final Logger logger = LoggerFactory.getLogger(ServerBootstrapper.class);

    @ConfigProperty(name = "cluster.hosts")
    private String clusterHosts;

    void onStart(@Observes StartupEvent ev) {
        assert this.clusterHosts != null;

        RaftServer instance = RaftServer.getInstance();

        ClusterState clusterState = new ClusterState(Arrays.asList(this.clusterHosts.split(",")));
        instance.setClusterState(clusterState);

        Scheduler scheduler = new Scheduler(10000, 30000, 500);
        instance.setScheduler(scheduler);

        logger.info("Starting server with uuid {}", instance.getUuid());
        instance.start();
        logger.info("Server with uuid {} started!", instance.getUuid());
    }
}
