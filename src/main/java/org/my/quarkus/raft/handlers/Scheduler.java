package org.my.quarkus.raft.handlers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Scheduler {
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

    public synchronized void startSendingHeartbeats() {
        scheduledFuture = heartbeatExecutorService.scheduleAtFixedRate(
                new HeartbeatHandler(),
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
