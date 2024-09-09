package org.my.raft.model.log;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LogTests {

    @Test
    public void given_log_when_fromIndexIsNegative_then_exception() {
        Log log = new Log();
        Assertions.assertThrows(IllegalArgumentException.class, () -> log.lastLogEntries(-1));
    }

    @Test
    public void given_log_when_fromIndexIsGreaterThanSize_then_emptyList() {
        Log log = new Log();
        assert log.lastLogEntries(1).isEmpty();
    }

    @Test
    public void given_log_when_get_then_logEntryReturned() {
        Log log = new Log();
        log.append("key", "value", 1);

        assert log.get(0).isPresent();
        assert log.get(0).get().command().key().equals("key");
        assert log.get(0).get().command().value().equals("value");
        assert log.get(0).get().term() == 1;

        assert log.get(1).isEmpty();
        assert log.get(-1).isEmpty();
    }

    @Test
    public void given_log_when_append_then_logEntryAppended() {
        Log log = new Log();

        log.append("key", "value", 1);

        assert log.lastLogEntry().isPresent();
        assert log.lastLogEntry().get().command().key().equals("key");
        assert log.lastLogEntry().get().command().value().equals("value");
        assert log.lastLogEntry().get().term() == 1;

        assert log.lastLogEntries(0).size() == 1;
        assert log.lastLogEntries(0).get(0).command().key().equals("key");
        assert log.lastLogEntries(0).get(0).command().value().equals("value");
        assert log.lastLogEntries(0).get(0).term() == 1;
    }

    @Test
    public void given_log_when_empty_then_lastLogEntryMustBeEmpty() {
        Log log = new Log();

        assert log.lastLogEntry().isEmpty();

        assert log.lastLogEntries(0).isEmpty();
        assert log.lastLogEntries(1).isEmpty();
    }
}
