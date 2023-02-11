package com.inulogic.smallrye.reactive.messaging.pulsar.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class SmallryeReactiveMessagingPulsarProcessor {

    static final String FEATURE = "smallrye-reactive-messaging-pulsar";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
