package org.my.quarkus.raft.handlers;

import org.my.quarkus.raft.model.cluster.RaftServer;
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
    private ScheduledFuture<?> scheduledFuture;

    public Scheduler(int lowerBoundElectionTimeout, int upperBoundElectionTimeout, int heartbeatTimeout) {
        this.lowerBoundElectionTimeout = lowerBoundElectionTimeout;
        this.upperBoundElectionTimeout = upperBoundElectionTimeout;
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public void startLeaderElectionHandler() {
        int electionTimeout = (int) (Math.random() * (upperBoundElectionTimeout - lowerBoundElectionTimeout) + lowerBoundElectionTimeout);
        leaderExecutorService.scheduleAtFixedRate(
                new LeaderElectionHandler(),
                electionTimeout,
                electionTimeout,
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }

    // todo: do I really like this synchronized?
    public synchronized Future<?> scheduleNow(Runnable runnable) {
        return heartbeatExecutorService.submit(runnable);
    }

    public synchronized void startSendingHeartbeats() {
        scheduledFuture = heartbeatExecutorService.scheduleAtFixedRate(
                () -> {
                    try {
                        logger.info("Sending heartbeats to followers...");
                        RaftServer.getInstance().triggerHeartbeat();
                    } catch (Exception e) {
                        logger.error("Error while sending heartbeats", e);
                    }
                },
                heartbeatTimeout,
                heartbeatTimeout,
                TimeUnit.MILLISECONDS
        );

    }

    public synchronized void stopSendingHeartbeats() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }
}
