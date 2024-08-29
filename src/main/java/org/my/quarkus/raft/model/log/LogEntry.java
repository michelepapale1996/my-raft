package org.my.quarkus.raft.model.log;

import org.my.quarkus.raft.model.state.machine.StateMachineCommand;

public record LogEntry(StateMachineCommand command, int term, int index) {

}
