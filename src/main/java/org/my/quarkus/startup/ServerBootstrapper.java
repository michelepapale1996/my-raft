package org.my.quarkus.startup;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.my.quarkus.client.ServerRestClient;
import org.my.raft.server.LeaderElectionHandler;
import org.my.raft.server.RequestAcceptor;
import org.my.raft.server.Scheduler;
import org.my.raft.model.cluster.ClusterState;
import org.my.raft.server.RaftServer;
import org.my.raft.server.RequestExecutor;
import org.my.raft.server.ServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

        RequestAcceptor requestAcceptor = new RequestAcceptor(server);
        server.setRequestAcceptor(requestAcceptor);

        RequestExecutor requestExecutor = new RequestExecutor(buildRestClients(clusterState));
        server.setRequestExecutor(requestExecutor);

        LeaderElectionHandler leaderElectionHandler = new LeaderElectionHandler(server);
        server.setLeaderElectionHandler(leaderElectionHandler);

        logger.info("Starting server with uuid {}", server.getUuid());
        server.start();
        logger.info("Server with uuid {} started!", server.getUuid());
    }

    private Map<String, ServerClient> buildRestClients(ClusterState clusterState) {
        return clusterState.getOtherClusterNodes().stream().collect(
                Collectors.toMap(
                        clusterHost -> clusterHost,
                        clusterHost -> RestClientBuilder.newBuilder()
                                .baseUri(URI.create(clusterHost))
                                .connectTimeout(10, TimeUnit.MILLISECONDS)
                                .build(ServerRestClient.class)
                )
        );
    }
}
