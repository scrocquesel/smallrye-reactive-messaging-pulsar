package com.inulogic.smallrye.reactive.messaging.pulsar.runtime.devconsole;

import org.eclipse.microprofile.config.Config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

@Singleton
public class PulsarHttpPortFinder {
    String httpPort;

    void collect(@Observes StartupEvent event, Config config) {
        httpPort = config.getOptionalValue("pulsar-http-port", String.class).orElse(null);
    }
}
