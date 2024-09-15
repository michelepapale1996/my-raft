package org.my.raft.model.log;

import org.my.raft.model.state.machine.StateMachineCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface Log {

    int size();

    int append(String key, String value, int term);

    LogEntry entryAt(int index);
}
