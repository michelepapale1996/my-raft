package org.my.raft.model.log;

import org.my.raft.model.state.machine.StateMachineCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Log {
    private final List<LogEntry> logEntries = new ArrayList<>();

    public int size() {
        return logEntries.size();
    }

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

    public synchronized Optional<LogEntry> entryAt(int index) {
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
        return nextEntry(this.logEntries.size() - 1);
    }

    /**
     * Returns the log entries starting from the given index.
     * In case from index is negative, an IllegalArgumentException is thrown.
     */
    public synchronized Optional<LogEntry> nextEntry(int previousIndex) {
        if (previousIndex < 0) {
            throw new IllegalArgumentException("previousIndex cannot be negative, given " + previousIndex);
        }

        if (previousIndex >= logEntries.size()) {
            return Optional.empty();
        }

        return Optional.of(logEntries.subList(previousIndex, previousIndex + 1).get(0));
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
