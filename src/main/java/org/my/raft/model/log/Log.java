package org.my.raft.model.log;

import org.my.raft.model.state.machine.StateMachineCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Log {
    private final List<LogEntry> logEntries = new ArrayList<>();

    public synchronized int append(String key, String value, int term) {
        int offset = logEntries.size();
        logEntries.add(new LogEntry(new StateMachineCommand(key, value), term, offset));
        return offset;
    }

    // todo: to improve
    public synchronized void put(int index, LogEntry logEntry) {
        logEntries.set(index, logEntry);
        // remove all entries after index
        logEntries.subList(index + 1, logEntries.size()).clear();
    }

    public synchronized Optional<LogEntry> get(int index) {
        if (index < 0 || index >= logEntries.size()) {
            return Optional.empty();
        } else {
            return Optional.of(logEntries.get(index));
        }
    }

    public synchronized Optional<LogEntry> lastLogEntry() {
        if (logEntries.isEmpty()) {
            return Optional.empty();
        }
        List<LogEntry> entries = lastLogEntries(this.logEntries.size() - 1);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(entries.get(0));
    }

    /**
     * Returns the log entries starting from the given index.
     * In case from index is negative, an IllegalArgumentException is thrown.
     */
    public synchronized List<LogEntry> lastLogEntries(int fromIndex) {
        if (fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex cannot be negative, given " + fromIndex);
        }

        if (fromIndex >= logEntries.size()) {
            return Collections.emptyList();
        }

        return logEntries.subList(fromIndex, logEntries.size());
    }
}
