package com.inulogic.smallrye.reactive.messaging.pulsar.deployment;

import java.io.Closeable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
public class PulsarDevServicesProcessor {
    private static final Logger log = Logger.getLogger(PulsarDevServicesProcessor.class);

    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-pulsar";
    private static final int PULSAR_PORT = 6650;
    private static final int PULSAR_HTTP_PORT = 8080;

    private static final ContainerLocator pulsarContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL, PULSAR_PORT);
    private static final String PULSAR_PORT_PROP = "pulsar-port";
    private static final String PULSAR_HTTP_PORT_PROP = "pulsar-http-port";    

    static volatile RunningDevService devService;
    static volatile PulsarDevServiceCfg cfg;
    static volatile boolean first = true;

    @BuildStep
    public DevServicesResultBuildItem startPulsarDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            LaunchModeBuildItem launchMode,
            PulsarBuildTimeConfig pulsarBuildTimeConfig,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig) {

        PulsarDevServiceCfg configuration = getConfiguration(pulsarBuildTimeConfig);

        if (devService != null) {
            boolean shouldShutdownTheBroker = !configuration.equals(cfg);
            if (!shouldShutdownTheBroker) {
                return devService.toBuildItem();
            }
            shutdownBroker();
            cfg = null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Pulsar Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            RunningDevService newDevService = startPulsar(dockerStatusBuildItem, configuration, launchMode,
                    devServicesConfig.timeout);
            if (newDevService != null) {
                devService = newDevService;

                if (devService.isOwner()) {
                    log.info("Dev Services for Pulsar started.");
                    log.info("Other Quarkus applications in dev mode will find the broker automatically.");
                }
            }
            if (devService == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (devService == null) {
            return null;
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devService != null) {
                    shutdownBroker();

                    log.info("Dev Services for Pulsar shut down.");
                }
                first = true;
                devService = null;
                cfg = null;
            };
            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
            ((QuarkusClassLoader) cl.parent()).addCloseTask(closeTask);
        }
        cfg = configuration;
        return devService.toBuildItem();
    }

    private void shutdownBroker() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop the Pulsar", e);
            } finally {
                devService = null;
            }
        }
    }

    private RunningDevService startPulsar(DockerStatusBuildItem dockerStatusBuildItem,
            PulsarDevServiceCfg config, LaunchModeBuildItem launchMode,
            Optional<Duration> timeout) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting Dev Services for Pulsar, as it has been disabled in the config.");
            return null;
        }

        // Verify that we have Pulsar channels without host and port
        if (!hasPulsarChannelWithoutServiceUrl()) {
            log.debug("Not starting Dev Services for Pulsar, all the channels are configured.");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warn("Docker isn't working, please configure Pulsar location.");
            return null;
        }

        final Supplier<RunningDevService> defaultPulsarSupplier = () -> {
            ConfiguredPulsarContainer container = new ConfiguredPulsarContainer(
                DockerImageName.parse(config.imageName).asCompatibleSubstituteFor("apachepulsar/pulsar"),
                config.fixedExposedPort,
                config.fixedExposedHttpPort,
                launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName : null);

            // Starting the broker
            timeout.ifPresent(container::withStartupTimeout);
            container.start();
            return getRunningDevService(container.getContainerId(), container::close, container.getHost(),
                    container.getPort(), container.getHttpPort());
        };

        final Supplier<Integer> httpPort = () -> pulsarContainerLocator
                .locatePublicPort(config.serviceName, config.shared, launchMode.getLaunchMode(), PULSAR_HTTP_PORT)
                .orElse(0);

        return pulsarContainerLocator.locateContainer(config.serviceName, config.shared, launchMode.getLaunchMode())
                .map(containerAddress -> getRunningDevService(containerAddress.getId(), null,
                        containerAddress.getHost(),
                        containerAddress.getPort(), httpPort.get()))
                .orElseGet(defaultPulsarSupplier);
    }

    private RunningDevService getRunningDevService(String containerId, Closeable closeable, String host, int port, int httpPort) {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("mp.messaging.connector.smallrye-pulsar.serviceUrl", String.format("pulsar://localhost:%d", port));
        configMap.put(PULSAR_PORT_PROP, String.valueOf(port));
        configMap.put(PULSAR_HTTP_PORT_PROP, String.valueOf(httpPort));
        return new RunningDevService(SmallryeReactiveMessagingPulsarProcessor.FEATURE,
                containerId, closeable, configMap);
    }

    private boolean hasPulsarChannelWithoutServiceUrl() {
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            boolean isIncoming = name.startsWith("mp.messaging.incoming.");
            boolean isOutgoing = name.startsWith("mp.messaging.outgoing.");
            boolean isConnector = name.endsWith(".connector");
            boolean isConfigured = false;
            if ((isIncoming || isOutgoing) && isConnector) {
                String connectorValue = config.getValue(name, String.class);
                boolean isPulsar = connectorValue.equalsIgnoreCase("smallrye-pulsar");
                boolean hasServiceUrl = ConfigUtils.isPropertyPresent(name.replace(".connector", ".serviceUrl"));
                isConfigured = isPulsar && hasServiceUrl;
            }

            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }


    private PulsarDevServiceCfg getConfiguration(PulsarBuildTimeConfig cfg) {
        PulsarDevServicesBuildTimeConfig devServicesConfig = cfg.devservices;
        return new PulsarDevServiceCfg(devServicesConfig);
    }

    private static final class PulsarDevServiceCfg {

        public boolean shared;
        public String serviceName;
        public int fixedExposedHttpPort;
        public int fixedExposedPort;
        public String imageName;
        public boolean devServicesEnabled;

        public PulsarDevServiceCfg(PulsarDevServicesBuildTimeConfig devServicesConfig) {
            this.devServicesEnabled = devServicesConfig.enabled.orElse(true);
            this.imageName = devServicesConfig.imageName;
            this.fixedExposedPort = devServicesConfig.port.orElse(0);
            this.fixedExposedHttpPort = devServicesConfig.httpPort.orElse(0);
            this.shared = devServicesConfig.shared;
            this.serviceName = devServicesConfig.serviceName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(shared, serviceName, fixedExposedHttpPort, fixedExposedPort, imageName,
                    devServicesEnabled);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof PulsarDevServiceCfg))
                return false;
            PulsarDevServiceCfg other = (PulsarDevServiceCfg) obj;
            return shared == other.shared && Objects.equals(serviceName, other.serviceName)
                    && fixedExposedHttpPort == other.fixedExposedHttpPort && fixedExposedPort == other.fixedExposedPort
                    && Objects.equals(imageName, other.imageName) && devServicesEnabled == other.devServicesEnabled;
        }

    }

    private static final class ConfiguredPulsarContainer extends PulsarContainer {

        private final int port;
        private final int httpPort;

        private ConfiguredPulsarContainer(DockerImageName dockerImageName, int fixedExposedPort,
                int fixedExposedHttpPort,
                String serviceName) {
            super(dockerImageName);
            this.port = fixedExposedPort;
            this.httpPort = fixedExposedHttpPort;
            withNetwork(Network.SHARED);
            withExposedPorts(PULSAR_PORT, PULSAR_HTTP_PORT);
            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
            if (!dockerImageName.getRepository().endsWith("pulsar")) {
                throw new IllegalArgumentException("Only official pulsar images are supported");
            }
        }

        @Override
        protected void configure() {
            super.configure();
            if (port > 0) {
                addFixedExposedPort(port, PULSAR_PORT);
            }
            if (httpPort > 0) {
                addFixedExposedPort(httpPort, PULSAR_HTTP_PORT);
            }
        }

        public int getPort() {
            return getMappedPort(PULSAR_PORT);
        }

        public int getHttpPort() {
            return getMappedPort(PULSAR_HTTP_PORT);
        }
    }
}
