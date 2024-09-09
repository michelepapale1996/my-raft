package org.my.quarkus.startup;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.my.raft.handlers.LeaderElectionHandler;
import org.my.raft.handlers.RequestHandler;
import org.my.raft.handlers.Scheduler;
import org.my.raft.model.cluster.ClusterState;
import org.my.raft.model.cluster.RaftServer;
import org.my.raft.model.cluster.RequestExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@ApplicationScoped
public class ServerBootstrapper {

    private static final Logger logger = LoggerFactory.getLogger(ServerBootstrapper.class);

    @Inject
    private RaftServer server;

    @ConfigProperty(name = "cluster.hosts")
    private String clusterHosts;

    void onStart(@Observes StartupEvent ev) {
        assert this.clusterHosts != null;

        ClusterState clusterState = new ClusterState(Arrays.asList(this.clusterHosts.split(",")));
        server.setClusterState(clusterState);

        Scheduler scheduler = new Scheduler(server, 10000, 30000, 2000);
        server.setScheduler(scheduler);

        RequestHandler requestHandler = new RequestHandler(server);
        server.setRequestHandler(requestHandler);

        RequestExecutor requestExecutor = new RequestExecutor(server);
        server.setRequestExecutor(requestExecutor);

        LeaderElectionHandler leaderElectionHandler = new LeaderElectionHandler(server);
        server.setLeaderElectionHandler(leaderElectionHandler);

        logger.info("Starting server with uuid {}", server.getUuid());
        server.start();
        logger.info("Server with uuid {} started!", server.getUuid());
    }
}
