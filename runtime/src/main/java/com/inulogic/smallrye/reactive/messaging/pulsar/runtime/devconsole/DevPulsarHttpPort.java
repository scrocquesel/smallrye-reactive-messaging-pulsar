package com.inulogic.smallrye.reactive.messaging.pulsar.runtime.devconsole;

import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.impl.LazyValue;

public class DevPulsarHttpPort {
    private final LazyValue<String> httpPort;

    public DevPulsarHttpPort() {
        this.httpPort = new LazyValue<>(new Supplier<String>() {

            @Override
            public String get() {
                ArcContainer arcContainer = Arc.container();
                PulsarHttpPortFinder PulsarHttpPortFinder = arcContainer.instance(PulsarHttpPortFinder.class).get();

                return PulsarHttpPortFinder.httpPort;
            }
        });
    }

    public String getHttpPort() {
        return httpPort.get();
    }
}
