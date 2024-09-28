package org.my.quarkus.startup;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.my.raft.server.RaftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RaftServerStarter {

    private static final Logger logger = LoggerFactory.getLogger(RaftServerStarter.class);

    @Inject
    private RaftServer server;

    void onStart(@Observes StartupEvent ev) {
        logger.info("Starting server with uuid {}", server.getUuid());
        server.start();
        logger.info("Server with uuid {} started!", server.getUuid());
    }
}
