package org.my.quarkus.producers;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.my.quarkus.client.ServerRestClient;
import org.my.raft.model.ClusterState;
import org.my.raft.server.RaftServer;
import org.my.raft.server.RequestExecutor;
import org.my.raft.server.Scheduler;
import org.my.raft.server.ServerClient;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RaftServerProducer {

    @ConfigProperty(name = "cluster.hosts")
    private String clusterHosts;

    @ApplicationScoped
    public RaftServer raftServer() {
        assert this.clusterHosts != null;
        ClusterState clusterState = new ClusterState(Arrays.asList(this.clusterHosts.split(",")));
        Scheduler scheduler = new Scheduler( 10000, 30000, 2000);
        RequestExecutor requestExecutor = new RequestExecutor(buildRestClients(clusterState));

        return RaftServer.builder()
                .withClusterState(new ClusterState(Arrays.asList(this.clusterHosts.split(","))))
                .withScheduler(scheduler)
                .withRequestExecutor(requestExecutor)
                .build();
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
