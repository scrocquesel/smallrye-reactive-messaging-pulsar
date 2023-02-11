package com.inulogic.smallrye.reactive.messaging.pulsar.runtime.devconsole;

import java.util.function.Supplier;

public class DevPulsarHttpPortSupplier implements Supplier<DevPulsarHttpPort> {
    @Override
    public DevPulsarHttpPort get() {
        return new DevPulsarHttpPort();
    }
}
