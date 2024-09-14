package org.my.raft.server;

import org.my.raft.model.api.append.entries.AppendEntriesRequest;
import org.my.raft.model.api.append.entries.AppendEntriesResponse;
import org.my.raft.model.api.voting.RequestVoteRequest;
import org.my.raft.model.api.voting.RequestVoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RequestExecutor {
    private static final Logger logger = LoggerFactory.getLogger(RequestExecutor.class);
    private final Map<String, ServerClient> serverRestClientByHostName = new HashMap<>();

    public RequestExecutor(Map<String, ServerClient> serverRestClientByHostName) {
        this.serverRestClientByHostName.putAll(serverRestClientByHostName);
    }

    public Map<String, AppendEntriesResponse> performAppendEntriesRequests(Map<String, AppendEntriesRequest> appendEntriesRequestsForOtherHosts) {
        Map<String, AppendEntriesResponse> responsesByServer = new HashMap<>();
        for (Map.Entry<String, AppendEntriesRequest> entry: appendEntriesRequestsForOtherHosts.entrySet()) {
            String serverId = entry.getKey();
            AppendEntriesRequest appendEntriesRequest = entry.getValue();
            ServerClient serverRestClient = this.serverRestClientByHostName.get(serverId);

            try {
                responsesByServer.put(serverId, serverRestClient.appendEntries(appendEntriesRequest));
            } catch (Exception e) {
                logger.error("Error while sending request to server {}. It will be skipped.", serverId);
            }
        }
        return responsesByServer;
    }

    public List<RequestVoteResponse> performVoteRequest(RequestVoteRequest requestVoteRequest) {
        // todo: send request in parallel
        return this.serverRestClientByHostName.entrySet().stream()
                .map(entry -> {
                    String serverId = entry.getKey();
                    ServerClient serverRestClient = entry.getValue();

                    try {
                        return serverRestClient.requestVote(requestVoteRequest);
                    } catch (Exception e) {
                        logger.error("Error while sending request vote to {}", serverId);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
