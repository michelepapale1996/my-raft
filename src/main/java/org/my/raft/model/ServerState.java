package org.my.raft.model;

import org.my.raft.model.log.Log;
import org.my.raft.model.state.machine.StateMachine;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ServerState {
    private final UUID uuid;
    private final Log log;
    private final ServerRole status;
    private final StateMachine stateMachine;
    private final int currentTerm;
    private final int commitIndex;
    private final int lastAppliedIndex;

    private final Map<String, Integer> nextIndexByHost;

    private final Map<String, Integer> matchIndexByHost;

    public static ServerState of(UUID uuid,
                                 Log log,
                                 ServerRole status,
                                 StateMachine stateMachine,
                                 int currentTerm, int commitIndex, int lastAppliedIndex,
                                 Map<String, Integer> nextIndexByHost,
                                 Map<String, Integer> matchIndexByHost) {
        return new ServerState(uuid, log, status, stateMachine, currentTerm, commitIndex, lastAppliedIndex, nextIndexByHost, matchIndexByHost);
    }

    private ServerState(UUID uuid,
                        Log log,
                        ServerRole status,
                        StateMachine stateMachine,
                        int currentTerm,
                        int commitIndex,
                        int lastAppliedIndex,
                        Map<String, Integer> nextIndexByHost,
                        Map<String, Integer> matchIndexByHost) {
        this.uuid = uuid;
        this.log = log;
        this.status = status;
        this.stateMachine = stateMachine;
        this.currentTerm = currentTerm;
        this.commitIndex = commitIndex;
        this.lastAppliedIndex = lastAppliedIndex;
        this.nextIndexByHost = nextIndexByHost;
        this.matchIndexByHost = matchIndexByHost;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Log getLog() {
        return log;
    }

    public ServerRole getStatus() {
        return status;
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public int getLastAppliedIndex() {
        return lastAppliedIndex;
    }

    public Map<String, Integer> getNextIndexByHost() {
        return nextIndexByHost;
    }

    public Map<String, Integer> getMatchIndexByHost() {
        return matchIndexByHost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerState that = (ServerState) o;
        return currentTerm == that.currentTerm && Objects.equals(uuid, that.uuid) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, status, currentTerm);
    }

    @Override
    public String toString() {
        return "ServerState{" +
                "uuid=" + uuid +
                ", log=" + log +
                ", status=" + status +
                ", stateMachine=" + stateMachine +
                ", currentTerm=" + currentTerm +
                ", commitIndex=" + commitIndex +
                ", lastAppliedIndex=" + lastAppliedIndex +
                ", nextIndexByHost=" + nextIndexByHost +
                ", matchIndexByHost=" + matchIndexByHost +
                '}';
    }
}
