package org.my.raft.model.log;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LogTests {

    @Test
    public void given_log_when_previousIndexIsNegative_then_exception() {
        Log log = new Log();
        Assertions.assertThrows(IllegalArgumentException.class, () -> log.nextEntry(-1));
    }

    @Test
    public void given_log_when_previousIndexIsGreaterThanSize_then_emptyList() {
        Log log = new Log();
        assert log.nextEntry(1).isEmpty();
    }

    @Test
    public void given_log_when_entryAt_then_logEntryReturned() {
        Log log = new Log();
        log.append("key", "value", 1);

        assert log.entryAt(0).isPresent();
        assert log.entryAt(0).get().command().key().equals("key");
        assert log.entryAt(0).get().command().value().equals("value");
        assert log.entryAt(0).get().term() == 1;

        assert log.entryAt(1).isEmpty();
        assert log.entryAt(-1).isEmpty();
    }

    @Test
    public void given_log_when_append_then_logEntryAppended() {
        Log log = new Log();

        log.append("key", "value", 1);

        assert log.lastLogEntry().isPresent();
        assert log.lastLogEntry().get().command().key().equals("key");
        assert log.lastLogEntry().get().command().value().equals("value");
        assert log.lastLogEntry().get().term() == 1;

        assert log.nextEntry(0).isPresent();
        assert log.nextEntry(0).get().command().key().equals("key");
        assert log.nextEntry(0).get().command().value().equals("value");
        assert log.nextEntry(0).get().term() == 1;
    }

    @Test
    public void given_log_when_empty_then_lastLogEntryMustBeEmpty() {
        Log log = new Log();

        assert log.lastLogEntry().isEmpty();

        assert log.nextEntry(0).isEmpty();
        assert log.nextEntry(1).isEmpty();
    }
}
