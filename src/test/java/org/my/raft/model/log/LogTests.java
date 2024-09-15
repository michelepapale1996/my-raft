package org.my.raft.model.log;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LogTests {

    @Test
    public void given_log_when_indexIsNegative_then_exception() {
        Log log = new Log();

        assert log.size() == 0;
        Assertions.assertThrows(IllegalArgumentException.class, () -> log.entryAt(-1));
    }

    @Test
    public void given_log_when_indexIsGreaterThanSize_then_exception() {
        Log log = new Log();

        assert log.size() == 0;
        Assertions.assertThrows(IllegalArgumentException.class, () -> log.entryAt(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> log.entryAt(1));
    }

    @Test
    public void given_log_when_entryAt_then_logEntryReturned() {
        Log log = new Log();
        log.append("key", "value", 1);

        assert log.size() == 1;
        assert log.entryAt(0) != null;
        assert log.entryAt(0).command().key().equals("key");
        assert log.entryAt(0).command().value().equals("value");
        assert log.entryAt(0).term() == 1;

        Assertions.assertThrows(IllegalArgumentException.class, () -> log.entryAt(1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> log.entryAt(-1));
    }
}
