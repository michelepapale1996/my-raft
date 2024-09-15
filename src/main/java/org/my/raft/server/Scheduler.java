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
    private volatile ScheduledFuture<?> scheduledFuture;

    public Scheduler(int lowerBoundElectionTimeout, int upperBoundElectionTimeout, int heartbeatTimeout) {
        this.lowerBoundElectionTimeout = lowerBoundElectionTimeout;
        this.upperBoundElectionTimeout = upperBoundElectionTimeout;
        this.heartbeatTimeout = heartbeatTimeout;
    }

    void startLeaderElectionHandler(Runnable task) {
        // todo: in this way the timeout is static
        int electionTimeout = (int) (Math.random() * (upperBoundElectionTimeout - lowerBoundElectionTimeout) + lowerBoundElectionTimeout);

        leaderExecutorService.scheduleAtFixedRate(
                task,
                electionTimeout,
                electionTimeout,
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }

    Future<?> scheduleNow(Runnable runnable) {
        return heartbeatExecutorService.submit(runnable);
    }

    synchronized void startSendingHeartbeats(Runnable task) {
        scheduledFuture = heartbeatExecutorService.scheduleAtFixedRate(
                task,
                heartbeatTimeout,
                heartbeatTimeout,
                TimeUnit.MILLISECONDS
        );

    }

    synchronized void stopSendingHeartbeats() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }
}
