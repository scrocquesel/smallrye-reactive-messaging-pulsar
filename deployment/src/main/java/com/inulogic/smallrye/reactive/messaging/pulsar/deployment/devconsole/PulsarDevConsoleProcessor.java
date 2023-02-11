package com.inulogic.smallrye.reactive.messaging.pulsar.deployment.devconsole;

import com.inulogic.smallrye.reactive.messaging.pulsar.runtime.devconsole.DevPulsarHttpPortSupplier;
import com.inulogic.smallrye.reactive.messaging.pulsar.runtime.devconsole.PulsarHttpPortFinder;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;

public class PulsarDevConsoleProcessor {
    
    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectInfos(CurateOutcomeBuildItem curatedOutcomeBuildItem){
        return new DevConsoleRuntimeTemplateInfoBuildItem("pulsarHttpPort", new DevPulsarHttpPortSupplier(), this.getClass(), curatedOutcomeBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.unremovableOf(PulsarHttpPortFinder.class);
    }
}
