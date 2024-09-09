package org.my.raft.model.cluster;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.my.quarkus.client.ServerRestClient;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class ClusterState {

    private static final int ME = 1;
    private final Set<String> otherClusterNodes = new HashSet<>();
    private final Map<String, ServerRestClient> serverRestClientByHostName = new HashMap<>();

    public ClusterState(List<String> clusterNodes) {
        assert clusterNodes != null;
        assert clusterNodes.size() > 0;
        // todo: check that each cluster host is a valid URI
        this.otherClusterNodes.addAll(clusterNodes);

        serverRestClientByHostName.putAll(
                otherClusterNodes.stream().collect(
                        Collectors.toMap(
                                clusterHost -> clusterHost,
                                clusterHost -> RestClientBuilder.newBuilder()
                                        .baseUri(URI.create(clusterHost))
                                        .connectTimeout(10, TimeUnit.MILLISECONDS)
                                        .build(ServerRestClient.class)
                        )
                )
        );
    }

    public int getClusterSize() {
        return otherClusterNodes.size() + ME;
    }

    public Map<String, ServerRestClient> getServerRestClientsByHostName() {
        return serverRestClientByHostName;
    }
}
