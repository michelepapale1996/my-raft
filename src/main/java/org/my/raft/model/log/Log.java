package org.my.raft.model.log;

public interface Log {

    int size();

    int append(String key, String value, int term);

    LogEntry entryAt(int index);
}
