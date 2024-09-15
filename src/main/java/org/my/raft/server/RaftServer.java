package org.my.raft.server;

import jakarta.ws.rs.NotFoundException;
import org.my.raft.model.ServerRole;
import org.my.raft.model.ServerState;
import org.my.raft.model.api.append.entries.AppendEntriesRequest;
import org.my.raft.model.api.append.entries.AppendEntriesResponse;
import org.my.raft.model.ClusterState;
import org.my.raft.model.api.voting.RequestVoteRequest;
import org.my.raft.model.api.voting.RequestVoteResponse;
import org.my.raft.model.log.InMemoryLog;
import org.my.raft.model.log.Log;
import org.my.raft.model.log.LogEntry;
import org.my.raft.model.state.machine.StateMachine;
import org.my.raft.model.state.machine.StateMachineCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RaftServer {

    private static final Logger logger = LoggerFactory.getLogger(RaftServer.class);
    private Scheduler scheduler;
    private RequestExecutor requestExecutor;
    private ClusterState clusterState;

    // fields used by all servers
    private final UUID uuid = UUID.randomUUID();
    private volatile ServerRole status = ServerRole.FOLLOWER;
    private final AtomicInteger currentTerm = new AtomicInteger(0);
    // index of the highest log entry known to be committed (initialized to 0, increases monotonically)
    private final AtomicInteger commitIndex = new AtomicInteger(-1);
    // index of the highest log entry applied to state machine (initialized to 0, increases monotonically)
    private final AtomicInteger lastApplied = new AtomicInteger(-1);
    private final Log log = new InMemoryLog();
    private final StateMachine stateMachine = new StateMachine();

    // fields used by leaders

    // for each server, index of the next log entry to send to that server
    // (initialized to leader last log index + 1)
    private final Map<String, Integer> nextIndexByHost = new ConcurrentHashMap<>();
    // for each server, index of the highest log entry known to be replicated on server
    // (initialized to 0, increases monotonically)
    private final Map<String, Integer> matchIndexByHost = new ConcurrentHashMap<>();


    // fields used by followers and candidates
    private volatile Optional<String> votedFor = Optional.empty();
    private static final AtomicBoolean receivedHeartbeat = new AtomicBoolean(false);


    public void setRequestExecutor(RequestExecutor requestExecutor) {
        this.requestExecutor = requestExecutor;
    }

    public RequestExecutor getRequestExecutor() {
        return requestExecutor;
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

    public int getCurrentTerm() {
        return currentTerm.get();
    }

    private void setCurrentTerm(int term) {
        currentTerm.set(term);
    }

    boolean isLeader() {
        return status == ServerRole.LEADER;
    }

    private void switchToFollower() {
        status = ServerRole.FOLLOWER;
        scheduler.stopSendingHeartbeats();
    }

    void switchToCandidate() {
        status = ServerRole.CANDIDATE;
        currentTerm.incrementAndGet();
        votedFor = Optional.empty();
    }

    void switchToLeader() {
        status = ServerRole.LEADER;

        // initialize nextIndexByHost and matchIndexByHost
        for (String serverId: clusterState.getOtherClusterNodes()) {
            nextIndexByHost.put(serverId, log.size());
            matchIndexByHost.put(serverId, -1);
        }

        scheduler.startSendingHeartbeats();
    }

    boolean hasReceivedHeartbeat() {
        return receivedHeartbeat.get();
    }

    void resetReceivedHeartbeat() {
        receivedHeartbeat.set(false);
    }

    Log getLog() {
        return log;
    }

    public ServerState getServerState() {
        ServerState serverState = ServerState.of(uuid, log, status, stateMachine, currentTerm.get(),
                commitIndex.get(), lastApplied.get(), nextIndexByHost, matchIndexByHost);
        logger.info("Current ServerState: {}", serverState);
        return serverState;
    }

    public void start() {
        assert clusterState != null;
        assert scheduler != null;
        assert requestExecutor != null;

        // todo: add a checker to perform assertions as:
        // - the cluster state is not empty
        // - the requestExecutor contains all the servers in the cluster state

        scheduler.startLeaderElectionHandler();
    }

    public void setStateMachineCommand(StateMachineCommand command) {
        logger.info("Received set request with command: {}", command);

        if (!isLeader()) {
            throw new IllegalStateException("I'm not a leader, I cannot set a value");
        }
        int offset = log.append(command.key(), command.value(), currentTerm.get());

        logger.info("Sending append entries to followers");
        triggerHeartbeat();

        try {
            // block thread till lastApplied is not set to currentTerm
            if (log.size() == 0) {
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

    public StateMachineCommand getStateMachineCommand(String key) {
        Optional<String> optionalValue = this.stateMachine.get(key);
        if (optionalValue.isEmpty()) {
            throw new NotFoundException();
        }
        return new StateMachineCommand(key, optionalValue.get());
    }

    private Map<String, AppendEntriesRequest> buildAppendEntriesRequests() {
        Map<String, AppendEntriesRequest> appendEntriesRequests = new HashMap<>();

        for (String serverId: clusterState.getOtherClusterNodes()) {
            if (!nextIndexByHost.containsKey(serverId) || !matchIndexByHost.containsKey(serverId)) {
                logger.error("nextIndexByHost or matchIndexByHost not initialized for server {}", serverId);
                throw new IllegalStateException();
            }

            AppendEntriesRequest appendEntriesRequest;
            // in case the log is empty, send just an heartbeat
            if (log.size() == 0) {
                appendEntriesRequest = new AppendEntriesRequest(
                        currentTerm.get(),
                        uuid.toString(),
                        -1,
                        -1,
                        Collections.emptyList(),
                        commitIndex.get()
                );
            } else {
                int lastLogIndex = log.size() - 1;
                int lastLogTerm = log.entryAt(lastLogIndex).term();
                // check if the follower is lagging behind or is up-to-date
                if (lastLogIndex >= nextIndexByHost.get(serverId)) {
                    // follower is missing some log entries. Send the first missing entry

                    List<LogEntry> entries = Collections.singletonList(log.entryAt(nextIndexByHost.get(serverId)));
                    if (nextIndexByHost.get(serverId) > 0) {
                        LogEntry lastLogEntryOnFollowerLog = log.entryAt(nextIndexByHost.get(serverId) - 1);
                        appendEntriesRequest = new AppendEntriesRequest(
                                currentTerm.get(),
                                uuid.toString(),
                                lastLogEntryOnFollowerLog.index(),
                                lastLogEntryOnFollowerLog.term(),
                                entries,
                                commitIndex.get()
                        );
                    } else {
                        appendEntriesRequest = new AppendEntriesRequest(
                                currentTerm.get(),
                                uuid.toString(),
                                -1,
                                -1,
                                entries,
                                commitIndex.get()
                        );
                    }
                } else {
                    // send an heartbeat since the follower is already up-to-date
                    appendEntriesRequest = new AppendEntriesRequest(
                            currentTerm.get(),
                            uuid.toString(),
                            lastLogIndex,
                            lastLogTerm,
                            Collections.emptyList(),
                            commitIndex.get()
                    );
                }
            }

            appendEntriesRequests.put(serverId, appendEntriesRequest);
        }
        return appendEntriesRequests;
    }


    /**
     * This method is called by the leader to send heartbeats to followers
     * and to replicate log entries
     */
    void triggerHeartbeat() {
        Map<String, AppendEntriesRequest> appendEntriesRequestsForOtherHosts = buildAppendEntriesRequests();
        logger.info("Computed append entries requests for other hosts: {}", appendEntriesRequestsForOtherHosts);

        scheduler.scheduleNow(
            () -> {
                try {
                    Map<String, AppendEntriesResponse> responsesByServer = requestExecutor.performAppendEntriesRequests(appendEntriesRequestsForOtherHosts);

                    int maxTerm = responsesByServer.values().stream()
                            .mapToInt(AppendEntriesResponse::term)
                            .max()
                            .orElse(this.currentTerm.get());

                    if (maxTerm > this.currentTerm.get()) {
                        logger.info("Received heartbeat response with a higher term. I will become a follower");
                        this.switchToFollower();
                        this.setCurrentTerm(maxTerm);
                    } else {

                        for (Map.Entry<String, AppendEntriesResponse> entry : responsesByServer.entrySet()) {
                            String serverId = entry.getKey();
                            AppendEntriesResponse response = entry.getValue();

                            maxTerm = Math.max(maxTerm, response.term());

                            if (response.success()) {
                                AppendEntriesRequest requestSent = appendEntriesRequestsForOtherHosts.get(serverId);
                                if (requestSent.entries().isEmpty()) {
                                    continue;
                                }
                                LogEntry lastAcceptedEntry = requestSent.entries().get(requestSent.entries().size() - 1);
                                int nextIndex = lastAcceptedEntry.index() + 1;
                                int matchIndex = lastAcceptedEntry.index();
                                logger.info("Received success response from follower {}. " +
                                        "NextIndex: {}; matchIndex: {}", serverId, nextIndex, matchIndex);
                                nextIndexByHost.put(serverId, nextIndex);
                                matchIndexByHost.put(serverId, matchIndex);
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
                        if (N > commitIndex.get() && N < log.size() && log.entryAt(N).term() == currentTerm.get()) {
                            commitIndex.set(N);
                        }

                        this.applyCommandToStateMachine();
                    }
                } catch (Exception e) {
                    logger.error("Error while sending heartbeat", e);
                }
            }
        );
    }

    /**
     * This method is called by followers to accept append entries requests
     */
    public synchronized AppendEntriesResponse acceptAppendEntries(AppendEntriesRequest appendEntriesRequest) {
        logger.info(appendEntriesRequest.toString());

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

        // edge case: if the log is empty, accept the request if the prevLogIndex is -1
        if (log.size() == 0) {
            if (appendEntriesRequest.prevLogIndex() == -1) {
                for (LogEntry entry: appendEntriesRequest.entries()) {
                    logger.info("Appending first command {} on follower log", entry.command());
                    log.append(entry.command().key(), entry.command().value(), this.currentTerm.get());
                }
                return new AppendEntriesResponse(this.currentTerm.get(), true);
            } else {
                // the log is empty and the prevLogIndex is not -1 - I'm not in sync!
                return new AppendEntriesResponse(this.currentTerm.get(), false);
            }
        }

        if (appendEntriesRequest.prevLogIndex() < log.size() &&
                log.entryAt(appendEntriesRequest.prevLogIndex()).term() != appendEntriesRequest.prevLogTerm()) {
            return new AppendEntriesResponse(this.currentTerm.get(), false);
        } else {
            for (LogEntry entry: appendEntriesRequest.entries()) {
                logger.info("Appending command {} on follower log", entry.command());
                log.append(entry.command().key(), entry.command().value(), this.currentTerm.get());
            }

            if (appendEntriesRequest.leaderCommit() > commitIndex.get()) {
                int lastNewLogEntryIndex = log.size() - 1;
                int lastCommittedOffset = Math.min(appendEntriesRequest.leaderCommit(), lastNewLogEntryIndex);
                logger.info("Setting commit index to {}", lastCommittedOffset);
                commitIndex.set(lastCommittedOffset);
            }

            this.applyCommandToStateMachine();

            return new AppendEntriesResponse(this.currentTerm.get(), true);
        }
    }

    public RequestVoteResponse requestVote(RequestVoteRequest requestVoteRequest) {
        logger.info(requestVoteRequest.toString());

        if (requestVoteRequest.term() < this.getCurrentTerm()) {
            return new RequestVoteResponse(this.getCurrentTerm(), false);
        }

        Optional<String> votedForOptional = this.votedFor;

        if ((votedForOptional.isEmpty() || votedForOptional.get().equals(requestVoteRequest.candidateId())) &&
                (requestVoteRequest.lastLogIndex() >= this.log.size() || this.log.entryAt(requestVoteRequest.lastLogIndex()).term() == requestVoteRequest.lastLogTerm())) {
            this.switchToFollower();
            this.setCurrentTerm(requestVoteRequest.term());
            this.votedFor = Optional.of(requestVoteRequest.candidateId());
            return new RequestVoteResponse(requestVoteRequest.term(), true);
        }
        return new RequestVoteResponse(this.getCurrentTerm(), false);
    }

    private void applyCommandToStateMachine() {
        // If commitIndex > lastApplied: increment lastApplied, apply log[lastApplied] to state machine
        if (commitIndex.get() > lastApplied.get()) {
            for (int i = lastApplied.get() + 1; i <= commitIndex.get(); i++) {
                LogEntry logEntry = log.entryAt(i);
                logger.info("Applying command {} on this server...", logEntry.command());
                stateMachine.apply(logEntry.command());
            }
            lastApplied.set(commitIndex.get());
        }
    }

}
