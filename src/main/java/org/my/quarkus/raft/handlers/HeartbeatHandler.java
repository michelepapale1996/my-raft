package org.my.quarkus.raft.handlers;

import org.my.quarkus.raft.api.AppendEntriesRequest;
import org.my.quarkus.raft.api.AppendEntriesResponse;
import org.my.quarkus.raft.model.RaftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class HeartbeatHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);
    private final RaftServer raftServer = RaftServer.getInstance();

    @Override
    public void run() {
        logger.info("Sending heartbeats to followers...");
        AppendEntriesRequest appendEntriesRequest = new AppendEntriesRequest(raftServer.getCurrentTerm(), raftServer.getUuid());
        List<AppendEntriesResponse> appendEntriesResponses = raftServer.getClusterState().getServerRestClients().stream().map(
                        serverRestClient -> {
                            try {
                                return serverRestClient.appendEntries(appendEntriesRequest);
                            } catch (Exception e) {
                                logger.error("Error while sending heartbeat");
                                return null;
                            }
                        })
                .filter(Objects::nonNull)
                .toList();

        long maxTerm = appendEntriesResponses.stream()
                .mapToLong(AppendEntriesResponse::term)
                .max()
                .orElse(raftServer.getCurrentTerm());

        if (maxTerm > raftServer.getCurrentTerm()) {
            logger.info("Received heartbeat response with a higher term. I will become a follower");
            raftServer.switchToFollower();
            raftServer.setCurrentTerm(maxTerm);
        }
    }
}
