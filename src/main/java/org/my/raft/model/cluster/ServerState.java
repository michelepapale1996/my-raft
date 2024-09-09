package org.my.raft.model.cluster;

import org.my.raft.model.log.Log;
import org.my.raft.model.state.machine.StateMachine;

import java.util.Objects;
import java.util.UUID;

public final class ServerState {
    private final UUID uuid;
    private final Log log;
    private final RaftServer.ServerRole status;
    private final StateMachine stateMachine;
    private final int currentIndex;
    private final int commitIndex;
    private final int lastAppliedIndex;

    public static ServerState of(UUID uuid,
                                 Log log,
                                 RaftServer.ServerRole status,
                                 StateMachine stateMachine,
                                 int currentIndex, int commitIndex, int lastAppliedIndex) {
        return new ServerState(uuid, log, status, stateMachine, currentIndex, commitIndex, lastAppliedIndex);
    }

    private ServerState(UUID uuid,
                        Log log,
                        RaftServer.ServerRole status,
                        StateMachine stateMachine,
                        int currentIndex,
                        int commitIndex,
                        int lastAppliedIndex) {
        this.uuid = uuid;
        this.log = log;
        this.status = status;
        this.stateMachine = stateMachine;
        this.currentIndex = currentIndex;
        this.commitIndex = commitIndex;
        this.lastAppliedIndex = lastAppliedIndex;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Log getLog() {
        return log;
    }

    public RaftServer.ServerRole getStatus() {
        return status;
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public int getLastAppliedIndex() {
        return lastAppliedIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ServerState) obj;
        return Objects.equals(this.uuid, that.uuid) &&
                Objects.equals(this.log, that.log) &&
                Objects.equals(this.status, that.status) &&
                Objects.equals(this.stateMachine, that.stateMachine) &&
                this.currentIndex == that.currentIndex &&
                this.commitIndex == that.commitIndex &&
                this.lastAppliedIndex == that.lastAppliedIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, log, status, stateMachine, currentIndex, commitIndex, lastAppliedIndex);
    }

    @Override
    public String toString() {
        return "ServerState[" +
                "uuid=" + uuid + ", " +
                "log=" + log + ", " +
                "status=" + status + ", " +
                "stateMachine=" + stateMachine + ", " +
                "currentIndex=" + currentIndex + ", " +
                "commitIndex=" + commitIndex + ", " +
                "lastAppliedIndex=" + lastAppliedIndex + ']';
    }


}
