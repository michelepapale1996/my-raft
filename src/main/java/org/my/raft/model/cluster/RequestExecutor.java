package org.my.raft.model.cluster;

import org.my.quarkus.client.ServerRestClient;
import org.my.raft.api.append.entries.AppendEntriesRequest;
import org.my.raft.api.append.entries.AppendEntriesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class RequestExecutor {
    private static final Logger logger = LoggerFactory.getLogger(RequestExecutor.class);
    private final ClusterState clusterState;

    public RequestExecutor(RaftServer raftServer) {
        this.clusterState = raftServer.getClusterState();
    }

    public Map<String, AppendEntriesResponse> performRequests(Map<String, AppendEntriesRequest> appendEntriesRequestsForOtherHosts) {
        Map<String, AppendEntriesResponse> responsesByServer = new HashMap<>();
        for (Map.Entry<String, AppendEntriesRequest> entry: appendEntriesRequestsForOtherHosts.entrySet()) {
            String serverId = entry.getKey();
            AppendEntriesRequest appendEntriesRequest = entry.getValue();
            ServerRestClient serverRestClient = this.clusterState.getServerRestClientsByHostName().get(serverId);

            try {
                responsesByServer.put(serverId, serverRestClient.appendEntries(appendEntriesRequest));
            } catch (Exception e) {
                logger.error("Error while sending request to server {}. It will be skipped.", serverId);
            }
        }
        return responsesByServer;
    }
}
