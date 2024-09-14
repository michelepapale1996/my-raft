package org.my.raft.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Scheduler {
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);
    private final ScheduledExecutorService leaderExecutorService = Executors.newScheduledThreadPool(1);
    private final ScheduledExecutorService heartbeatExecutorService = Executors.newScheduledThreadPool(1);
    private final int lowerBoundElectionTimeout;
    private final int upperBoundElectionTimeout;
    private final int heartbeatTimeout;
    private final RaftServer raftServer;
    private volatile ScheduledFuture<?> scheduledFuture;

    public Scheduler(RaftServer server, int lowerBoundElectionTimeout, int upperBoundElectionTimeout, int heartbeatTimeout) {
        this.raftServer = server;
        this.lowerBoundElectionTimeout = lowerBoundElectionTimeout;
        this.upperBoundElectionTimeout = upperBoundElectionTimeout;
        this.heartbeatTimeout = heartbeatTimeout;
    }

    void startLeaderElectionHandler() {
        // todo: in this way the timeout is static
        int electionTimeout = (int) (Math.random() * (upperBoundElectionTimeout - lowerBoundElectionTimeout) + lowerBoundElectionTimeout);
        LeaderElectionHandler leaderElectionHandler = new LeaderElectionHandler(this.raftServer);
        leaderExecutorService.scheduleAtFixedRate(
                () -> {
                    try {
                        // if I'm the leader, I don't need to trigger an election
                        if (this.raftServer.isLeader()) {
                            logger.info("I'm the leader, no need to trigger an election");
                            return;
                        }

                        if (!raftServer.hasReceivedHeartbeat()) {
                            logger.info("Starting election since I've not received the heartbeat...");
                            leaderElectionHandler.triggerElection();
                        }
                        raftServer.resetReceivedHeartbeat();
                    } catch (Exception e) {
                        logger.error("Error while running leader election handler", e);
                    }
                },
                electionTimeout,
                electionTimeout,
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }

    // todo: do I really like this synchronized?
    synchronized Future<?> scheduleNow(Runnable runnable) {
        return heartbeatExecutorService.submit(runnable);
    }

    synchronized void startSendingHeartbeats() {
        scheduledFuture = heartbeatExecutorService.scheduleAtFixedRate(
                () -> {
                    try {
                        logger.info("Sending heartbeats to followers...");
                        this.raftServer.triggerHeartbeat();
                    } catch (Exception e) {
                        logger.error("Error while sending heartbeats", e);
                    }
                },
                heartbeatTimeout,
                heartbeatTimeout,
                TimeUnit.MILLISECONDS
        );

    }

    synchronized void stopSendingHeartbeats() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }
}
