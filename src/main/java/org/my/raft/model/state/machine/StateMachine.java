package org.my.raft.model.state.machine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StateMachine {
    private final Map<String, String> state = new ConcurrentHashMap<>();

    public void apply(StateMachineCommand command) {
        state.put(command.key(), command.value());
    }

    public String get(String key) {
        return state.get(key);
    }

}
