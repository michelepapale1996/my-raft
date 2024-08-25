package org.my.quarkus.raft.model;

import org.my.quarkus.raft.handlers.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class RaftServer {
    private static final Logger logger = LoggerFactory.getLogger(RaftServer.class);

    enum ServerRole { LEADER, CANDIDATE, FOLLOWER }
    private ClusterState clusterState;
    private Scheduler scheduler;
    private final UUID uuid = UUID.randomUUID();
    private final AtomicLong currentTerm = new AtomicLong(0L);
    private volatile Optional<String> votedFor = Optional.empty();
    private volatile ServerRole status = ServerRole.FOLLOWER;
    private static final AtomicBoolean receivedHeartbeat = new AtomicBoolean(false);
    private final List<String> log = List.of();

    private RaftServer() {
        // private because of singleton - use getInstance()
    }

    private static class ServerLoader {
        private static final RaftServer INSTANCE = new RaftServer();
    }

    public static RaftServer getInstance() {
        return ServerLoader.INSTANCE;
    }

    public void start() {
        assert clusterState != null;
        assert scheduler != null;

        scheduler.startLeaderElectionHandler();
    }

    public void setClusterState(ClusterState clusterState) {
        this.clusterState = clusterState;
    }

    public ClusterState getClusterState() {
        return clusterState;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public String getUuid() {
        return this.uuid.toString();
    }

    public long getCurrentTerm() {
        return currentTerm.get();
    }

    public void setCurrentTerm(long term) {
        currentTerm.set(term);
    }

    public Optional<String> getVotedFor() {
        return votedFor;
    }

    public List<String> getLog() {
        return log;
    }

    public void setVotedFor(String candidateId) {
        votedFor = Optional.ofNullable(candidateId);
    }

    public boolean isLeader() {
        return status == ServerRole.LEADER;
    }

    public void switchToFollower() {
        status = ServerRole.FOLLOWER;
        scheduler.stopSendingHeartbeats();
    }

    public void switchToCandidate() {
        status = ServerRole.CANDIDATE;
        currentTerm.incrementAndGet();
        votedFor = Optional.empty();
    }

    public void switchToLeader() {
        status = ServerRole.LEADER;
        scheduler.startSendingHeartbeats();
    }

    public void setReceivedHeartbeat() {
        receivedHeartbeat.set(true);
    }

    public boolean hasReceivedHeartbeat() {
        return receivedHeartbeat.get();
    }

    public void resetReceivedHeartbeat() {
        receivedHeartbeat.set(false);
    }
}
