package org.my.quarkus.raft.model.cluster;

import org.my.quarkus.raft.api.AppendEntriesRequest;
import org.my.quarkus.raft.api.AppendEntriesResponse;
import org.my.quarkus.raft.client.ServerRestClient;
import org.my.quarkus.raft.handlers.Scheduler;
import org.my.quarkus.raft.model.log.Log;
import org.my.quarkus.raft.model.log.LogEntry;
import org.my.quarkus.raft.model.state.machine.StateMachine;
import org.my.quarkus.raft.model.state.machine.StateMachineCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RaftServer {

    private static final Logger logger = LoggerFactory.getLogger(RaftServer.class);
    private RaftServer() {
        // private because of singleton - use getInstance()
    }

    private static class ServerLoader {
        private static final RaftServer INSTANCE = new RaftServer();
    }

    public static RaftServer getInstance() {
        return ServerLoader.INSTANCE;
    }


    enum ServerRole { LEADER, CANDIDATE, FOLLOWER }
    private ClusterState clusterState;
    private Scheduler scheduler;


    // fields used by followers and candidates
    private volatile Optional<String> votedFor = Optional.empty();
    private static final AtomicBoolean receivedHeartbeat = new AtomicBoolean(false);


    // fields used by leaders

    // for each server, index of the next log entry to send to that server
    // (initialized to leader last log index + 1)
    private final Map<String, Integer> nextIndexByHost = new ConcurrentHashMap<>();
    // for each server, index of the highest log entry known to be replicated on server
    // (initialized to 0, increases monotonically)
    private final Map<String, Integer> matchIndexByHost = new ConcurrentHashMap<>();

    // fields used by all servers
    private final UUID uuid = UUID.randomUUID();
    private final Log log = new Log();
    private final StateMachine stateMachine = new StateMachine();
    private volatile ServerRole status = ServerRole.FOLLOWER;
    private final AtomicInteger currentTerm = new AtomicInteger(0);
    // index of the highest log entry known to be committed (initialized to 0, increases monotonically)
    private final AtomicInteger commitIndex = new AtomicInteger(-1);
    // index of the highest log entry applied to state machine (initialized to 0, increases monotonically)
    private final AtomicInteger lastApplied = new AtomicInteger(-1);

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

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public String getUuid() {
        return this.uuid.toString();
    }

    public int getCurrentTerm() {
        return currentTerm.get();
    }

    public void setCurrentTerm(int term) {
        currentTerm.set(term);
    }

    public Optional<String> getVotedFor() {
        return votedFor;
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

        // initialize nextIndexByHost and matchIndexByHost
        for (String serverId: clusterState.getServerRestClientsByHostName().keySet()) {
            Optional<LogEntry> entry = log.lastLogEntry();
            if (entry.isEmpty()) {
                nextIndexByHost.put(serverId, 0);
            } else {
                nextIndexByHost.put(serverId, entry.get().index() + 1);
            }
            matchIndexByHost.put(serverId, -1);
        }

        scheduler.startSendingHeartbeats();
    }

    public boolean hasReceivedHeartbeat() {
        return receivedHeartbeat.get();
    }

    public void resetReceivedHeartbeat() {
        receivedHeartbeat.set(false);
    }

    public Log getLog() {
        return log;
    }

    private Map<String, AppendEntriesRequest> buildAppendEntriesRequests() {
        Map<String, AppendEntriesRequest> appendEntriesRequests = new HashMap<>();

        Optional<LogEntry> logEntry = log.lastLogEntry();
        int prevLogIndex = 0;
        int prevLogTerm = 0;
        if (logEntry.isPresent()) {
            prevLogIndex = logEntry.get().index();
            prevLogTerm = logEntry.get().term();
        }

        for (Map.Entry<String, ServerRestClient> entry: clusterState.getServerRestClientsByHostName().entrySet()) {
            String serverId = entry.getKey();

            if (!nextIndexByHost.containsKey(serverId) || !matchIndexByHost.containsKey(serverId)) {
                logger.error("nextIndexByHost or matchIndexByHost not initialized for server {}", serverId);
                throw new IllegalStateException();
            }

            List<LogEntry> entries = new ArrayList<>();
            if (prevLogIndex >= nextIndexByHost.get(serverId)) {
                entries.addAll(log.lastLogEntries(nextIndexByHost.get(serverId)));
            }

            AppendEntriesRequest appendEntriesRequest = new AppendEntriesRequest(
                    currentTerm.get(),
                    uuid.toString(),
                    prevLogIndex,
                    prevLogTerm,
                    entries,
                    commitIndex.get()
            );

            appendEntriesRequests.put(serverId, appendEntriesRequest);
        }
        return appendEntriesRequests;
    }

    private Map<String, AppendEntriesResponse> performRequests(Map<String, AppendEntriesRequest> appendEntriesRequestsForOtherHosts) {
        Map<String, AppendEntriesResponse> responsesByServer = new HashMap<>();
        for (Map.Entry<String, AppendEntriesRequest> entry: appendEntriesRequestsForOtherHosts.entrySet()) {
            String serverId = entry.getKey();
            AppendEntriesRequest appendEntriesRequest = entry.getValue();
            ServerRestClient serverRestClient = clusterState.getServerRestClientsByHostName().get(serverId);

            try {
                responsesByServer.put(serverId, serverRestClient.appendEntries(appendEntriesRequest));
            } catch (Exception e) {
                logger.error("Error while sending request to server {}. It will be skipped.", serverId, e);
            }
        }
        return responsesByServer;
    }

    public Future<?> triggerHeartbeat() {
        Map<String, AppendEntriesRequest> appendEntriesRequestsForOtherHosts = buildAppendEntriesRequests();
        logger.info("Computed append entries requests for other hosts: {}", appendEntriesRequestsForOtherHosts);

        return scheduler.scheduleNow(
                () -> {
                    try {
                        Map<String, AppendEntriesResponse> responsesByServer = performRequests(appendEntriesRequestsForOtherHosts);

                        int maxTerm = responsesByServer.values().stream()
                                .mapToInt(AppendEntriesResponse::term)
                                .max()
                                .orElse(this.currentTerm.get());

                        if (maxTerm > this.currentTerm.get()) {
                            logger.info("Received heartbeat response with a higher term. I will become a follower");
                            this.switchToFollower();
                            this.setCurrentTerm(maxTerm);
                        } else {
                            Optional<LogEntry> lastLogEntry = log.lastLogEntry();

                            for (Map.Entry<String, AppendEntriesResponse> entry : responsesByServer.entrySet()) {
                                String serverId = entry.getKey();
                                AppendEntriesResponse response = entry.getValue();

                                maxTerm = Math.max(maxTerm, response.term());

                                if (response.success()) {
                                    if (lastLogEntry.isPresent()) {
                                        int nextIndex = lastLogEntry.get().index() + 1;
                                        int matchIndex = lastLogEntry.get().index();
                                        logger.info("Received success response from follower {}. " +
                                                "NextIndex: {}; matchIndex: {}", serverId, nextIndex, matchIndex);
                                        nextIndexByHost.put(serverId, nextIndex);
                                        matchIndexByHost.put(serverId, matchIndex);
                                    }
                                } else {
                                    int nextIndex = nextIndexByHost.get(serverId) - 1;
                                    int matchIndex = matchIndexByHost.get(serverId);
                                    logger.info("Received failure response from follower {}. " +
                                            "NextIndex: {}; matchIndex: {}", serverId, nextIndex, matchIndex);
                                    nextIndexByHost.put(serverId, nextIndex);
                                }
                            }

                            // If there exists an N such that N > commitIndex, a majority
                            // of matchIndex[i] ≥ N, and log[N].term == currentTerm: set commitIndex = N
                            List<Integer> matchIndexes = matchIndexByHost.values().stream().sorted().toList();
                            int N = matchIndexes.get(matchIndexes.size() / 2);
                            if (N > commitIndex.get() && log.get(N).isPresent() && log.get(N).get().term() == currentTerm.get()) {
                                commitIndex.set(N);
                            }

                            // If commitIndex > lastApplied: increment lastApplied, apply log[lastApplied] to state machine
                            if (commitIndex.get() > lastApplied.get()) {
                                for (int i = lastApplied.get() + 1; i <= commitIndex.get(); i++) {
                                    Optional<LogEntry> logEntry = log.get(i);
                                    logEntry.ifPresent(entry -> {
                                        logger.info("Applying command {} on leader", entry.command());
                                        stateMachine.apply(entry.command());
                                    });
                                }
                                lastApplied.set(commitIndex.get());
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error while sending heartbeat", e);
                    }
                }
        );
    }

    public void set(String key, String value) {
        if (!isLeader()) {
            throw new IllegalStateException("I'm not a leader, I cannot set a value");
        }
        int offset = log.append(key, value, currentTerm.get());

        logger.info("Sending append entries to followers");
        triggerHeartbeat();

        try {
            // block thread till lastApplied is not set to currentTerm
            if (log.lastLogEntry().isEmpty()) {
                throw new IllegalStateException("Last log entry is not present when setting a value");
            }

            while (lastApplied.get() != offset) {
                logger.info("Waiting for lastApplied to be set to {}", offset);
                Thread.sleep(500);
            }


        } catch (Exception e) {
            logger.error("Error sending append entries", e);
        }
    }

    public synchronized AppendEntriesResponse accept(AppendEntriesRequest appendEntriesRequest) {
        if (this.isLeader()) {
            logger.error("Received append entries request from another leader. Current leader is {}", this.getUuid());
            return new AppendEntriesResponse(this.getCurrentTerm(), false);
        }

        if (appendEntriesRequest.term() < this.getCurrentTerm()) {
            logger.error("Received append entries request with stale term ({}). Current term is {}",
                    appendEntriesRequest.term(), this.getCurrentTerm());
            return new AppendEntriesResponse(this.getCurrentTerm(), false);
        }

        // in all other cases, I'm a follower that has received either a heartbeat or an append entries request

        this.switchToFollower(); // if I was a candidate I will become a follower
        this.setCurrentTerm(appendEntriesRequest.term());

        receivedHeartbeat.set(true);

        if (log.get(appendEntriesRequest.prevLogIndex()).isPresent() &&
                log.get(appendEntriesRequest.prevLogIndex()).get().term() != appendEntriesRequest.prevLogTerm()) {
            return new AppendEntriesResponse(this.currentTerm.get(), false);
        } else {
            for (LogEntry entry: appendEntriesRequest.entries()) {
                logger.info("Appending command {} on follower log", entry.command());
                log.append(entry.command().key(), entry.command().value(), this.currentTerm.get());
            }

            if (appendEntriesRequest.leaderCommit() > commitIndex.get()) {
                int numberOfLogEntries = appendEntriesRequest.entries().size();
                int lastNewLogEntryIndex = 0;
                if (numberOfLogEntries > 0) {
                    lastNewLogEntryIndex = appendEntriesRequest.entries().get(numberOfLogEntries - 1).index();
                }

                int lastCommittedOffset = Math.min(appendEntriesRequest.leaderCommit(), lastNewLogEntryIndex);
                logger.info("Setting commit index to {}", lastCommittedOffset);
                commitIndex.set(lastCommittedOffset);
            }

            // apply the command to the state machine if commitIndex > lastApplied
            if (commitIndex.get() > lastApplied.get()) {
                for (int i = lastApplied.get() + 1; i <= commitIndex.get(); i++) {
                    Optional<LogEntry> logEntry = log.get(i);
                    logEntry.ifPresent(it -> {
                        logger.info("Applying command {} on follower", it.command());
                        stateMachine.apply(it.command());
                    });

                }
                lastApplied.set(commitIndex.get());
            }

            return new AppendEntriesResponse(this.currentTerm.get(), true);
        }
    }

}
