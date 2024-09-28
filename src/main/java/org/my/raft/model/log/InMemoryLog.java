package org.my.raft.model.log;

import org.my.raft.model.state.machine.StateMachineCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryLog implements Log {
    private final List<LogEntry> logEntries = new ArrayList<>();

    public int size() {
        return logEntries.size();
    }

    public synchronized int append(String key, String value, int term) {
        int offset = logEntries.size();
        logEntries.add(new LogEntry(new StateMachineCommand(key, value), term, offset));
        return offset;
    }

    public synchronized LogEntry entryAt(int index) {
        if (index < 0 || index >= logEntries.size()) {
            throw new IllegalArgumentException("Index out of bounds: " + index + " for log size: " + logEntries.size());
        }

        return logEntries.get(index);
    }

    @Override
    public String toString() {
        return "Log{" +
                "logEntries=" + logEntries +
                '}';
    }

    // used for serialization purposes. TODO: To be removed
    public List<LogEntry> getLogEntries() {
        return logEntries;
    }
}
