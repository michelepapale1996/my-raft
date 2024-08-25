package org.my.quarkus.raft.model;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.my.quarkus.raft.client.ServerRestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ClusterState {

    private final List<String> otherClusterHosts = new ArrayList<>();
    private final List<ServerRestClient> serverRestClients = new ArrayList<>();

    public ClusterState(List<String> clusterHosts) {
        assert clusterHosts != null;
        assert clusterHosts.size() > 0;
        // todo: check that each cluster host is a valid URI

        this.otherClusterHosts.addAll(clusterHosts);

        serverRestClients.addAll(
                otherClusterHosts.stream().map(clusterHost -> RestClientBuilder.newBuilder()
                        .baseUri(URI.create(clusterHost))
                        .connectTimeout(10, TimeUnit.MILLISECONDS)
                        .build(ServerRestClient.class)).toList()
        );
    }

    public int getClusterSize() {
        return otherClusterHosts.size() + 1;
    }
    public List<ServerRestClient> getServerRestClients() {
        return serverRestClients;
    }
}
