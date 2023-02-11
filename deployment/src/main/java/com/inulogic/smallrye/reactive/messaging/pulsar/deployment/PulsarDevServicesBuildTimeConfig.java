package com.inulogic.smallrye.reactive.messaging.pulsar.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PulsarDevServicesBuildTimeConfig {

    /**
     * If Dev Services for Pulsar has been explicitly enabled or disabled. Dev
     * Services are generally enabled
     * by default, unless there is an existing configuration present. For Pulsar,
     * Dev Services starts a broker unless all the Reactive
     * Messaging Pulsar channel are configured
     * with
     * {@code serviceUrl}.
     */
    @ConfigItem
    public Optional<Boolean> enabled = Optional.empty();

    /**
     * The image to use.
     * Note that only official Pulsar images are supported.
     * Specifically, the image repository must end with {@code pulsar}.
     */
    @ConfigItem(defaultValue = "apachepulsar/pulsar:2.10.0")
    public String imageName;

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @ConfigItem
    public Optional<Integer> port;

    /**
     * Optional fixed port for the Pulsar web service.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @ConfigItem
    public Optional<Integer> httpPort;

    /**
     * Indicates if Pulsar managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service
     * discovery.
     * If a matching container is found, it is used, and so a second one is not
     * started.
     * Otherwise, Dev Services for Pulsar starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-pulsar} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean shared;

    /**
     * The value of the {@code quarkus-dev-service-pulsar} label attached to the
     * started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Pulsar looks
     * for a container with the
     * {@code quarkus-dev-service-pulsar} label
     * set to the configured value. If found, it will use this container instead of
     * starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-pulsar} label
     * set to the specified value.
     * <p>
     * This property is used when you need multiple shared Pulsar brokers.
     */
    @ConfigItem(defaultValue = "pulsar")
    public String serviceName;

}
