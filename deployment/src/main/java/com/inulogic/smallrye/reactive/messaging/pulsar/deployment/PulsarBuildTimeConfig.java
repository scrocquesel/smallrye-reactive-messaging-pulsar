package com.inulogic.smallrye.reactive.messaging.pulsar.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "pulsar", phase = ConfigPhase.BUILD_TIME)
public class PulsarBuildTimeConfig {

    /**
     * Configuration for DevServices. DevServices allows Quarkus to automatically start a Pulsar broker in dev and test mode.
     */
    @ConfigItem
    public PulsarDevServicesBuildTimeConfig devservices;
    
}
