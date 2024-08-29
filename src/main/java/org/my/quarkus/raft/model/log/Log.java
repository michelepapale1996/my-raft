package org.my.quarkus.raft.model.log;

import org.my.quarkus.raft.model.state.machine.StateMachineCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Log {
    private final List<LogEntry> logEntries = new ArrayList<>();

    public synchronized void append(String key, String value, int term) {
        logEntries.add(new LogEntry(new StateMachineCommand(key, value), term, logEntries.size()));
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

    public synchronized List<LogEntry> lastLogEntries(int fromIndex) {
        return logEntries.subList(fromIndex, logEntries.size());
    }
}
