package org.my.raft.model.log;

import org.my.raft.model.state.machine.StateMachineCommand;

public record LogEntry(StateMachineCommand command, int term, int index) {
}
