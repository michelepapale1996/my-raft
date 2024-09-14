package org.my.raft.model.cluster;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ClusterState {

    private static final int ME = 1;
    private final Set<String> otherClusterNodes = new HashSet<>();

    public ClusterState(List<String> clusterNodeEndpoints) {
        assert clusterNodeEndpoints != null;
        assert clusterNodeEndpoints.size() > 0;
        // todo: check that each cluster host is a valid URI
        this.otherClusterNodes.addAll(clusterNodeEndpoints);
    }

    public int getClusterSize() {
        return otherClusterNodes.size() + ME;
    }

    public Set<String> getOtherClusterNodes() {
        return otherClusterNodes;
    }
}
