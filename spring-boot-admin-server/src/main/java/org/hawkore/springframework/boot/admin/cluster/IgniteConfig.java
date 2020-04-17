/*
 * Copyright 2020 HAWKORE, S.L.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkore.springframework.boot.admin.cluster;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteState;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.ImportResource;

/**
 * Ignite config
 *
 * @author Manuel Núñez Sánchez (manuel.nunez@hawkore.com)
 */
@Configuration
@ImportResource("${ignite.configFile}")
public class IgniteConfig implements DisposableBean, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(IgniteConfig.class);
    @Value("${ignite.activateCluster.topology}")
    private String activateClusterTopology;
    @Autowired
    private IgniteConfiguration igniteConfiguration;
    private Ignite ignite;

    /**
     * Ignite ignite.
     *
     * @return the ignite
     * @throws InterruptedException
     *     the interrupted exception
     */
    @Bean(name = "IgniteInstance")
    public Ignite ignite() throws InterruptedException {
        return ignite = createIgnite(igniteConfiguration, activateClusterTopology);
    }

    /**
     * Health inficator health indicator.
     *
     * @return the health indicator
     */
    @Bean(name = "IgniteClusterHealthIndicator")
    @DependsOn("IgniteInstance")
    public HealthIndicator healthInficator() {
        return new IgniteHealthIndicator();
    }

    /*
     * Create ignite ignite.
     */
    private Ignite createIgnite(IgniteConfiguration igniteConfiguration, String activateClusterTopology)
        throws InterruptedException {
        Ignite ignite = null;
        try {
            ignite = Ignition.ignite(igniteConfiguration.getIgniteInstanceName());
            logger.info("[createIgnite] Attached with next configuration: " + igniteConfiguration.toString());
        } catch (Exception e) {
            logger.info("[createIgnite] Starting with next configuration: " + igniteConfiguration.toString());
            ignite = Ignition.start(igniteConfiguration);
        }
        if (!ignite.cluster().active()) {
            if ("*".equals(activateClusterTopology)) {
                logger.info("[ignite] Activate cluster");
            } else {
                logger.info("[ignite] Activate cluster on topology: {}", activateClusterTopology);
                final Semaphore s = new Semaphore(0);
                ignite.events()
                    .localListen(new TopologyVerifier(igniteConfiguration.getGridName(), activateClusterTopology, s),
                        EventType.EVT_NODE_JOINED);
                s.acquire();
            }
            ignite.cluster().active(true);
        }
        logger.info("[createIgnite] Started and active!");
        return ignite;
    }

    /**
     * The type Ignite health indicator.
     */
    public class IgniteHealthIndicator extends AbstractHealthIndicator {

        /**
         * Do health check.
         *
         * @param builder
         *     the builder
         * @throws Exception
         *     the exception
         */
        @Override
        protected void doHealthCheck(Health.Builder builder) throws Exception {

            try {
                IgniteState state = ignite == null ? IgniteState.STOPPED : Ignition.state(ignite.name());
                switch (state) {
                    case STARTED:
                        builder.up();
                        break;
                    default:
                        builder.down();
                        break;
                }
                if (ignite != null) {
                    builder.withDetail("gridName", ignite.name());
                    if (state.equals(IgniteState.STARTED)) {
                        builder.withDetail("active", ignite.cluster().active());
                        builder.withDetail("numberOfClientNodes", ignite.cluster().forClients().hostNames().size());
                        builder.withDetail("numberOfServerNodes", ignite.cluster().forServers().hostNames().size());
                        builder.withDetail("numberOfDaemonNodes", ignite.cluster().forDaemons().hostNames().size());
                    }
                }
            } catch (Exception e) {
                builder.down();
                builder.withDetail("Unable to obtain ignite health status", e.getMessage());
            }
        }

    }

    // Topology verifier

    /**
     * The type Topology verifier.
     */
    public static class TopologyVerifier implements IgnitePredicate<DiscoveryEvent> {

        /**
         * The constant ATTR_IGNITE_NAME.
         */
        public static final String ATTR_IGNITE_NAME = "org.apache.ignite.ignite.name";
        private final String ignName;
        private final Semaphore s;
        private final List<String> nodes;

        /**
         * Instantiates a new Topology verifier.
         *
         * @param ignName
         *     the ign name
         * @param topology
         *     the topology
         * @param s
         *     the s
         */
        public TopologyVerifier(final String ignName, String topology, final Semaphore s) {
            this.nodes = Arrays.asList(topology.split(";"));
            this.s = s;
            this.ignName = ignName;
        }

        // IgnitePredicate

        /**
         * Apply boolean.
         *
         * @param event
         *     the event
         * @return the boolean
         */
        @Override
        public boolean apply(DiscoveryEvent event) {
            final String eventIgniteName = event.eventNode().attribute(ATTR_IGNITE_NAME);
            logger.info("[TopologyVerifier] {}: Node {} joined cluster", ignName, eventIgniteName);
            final Iterator<ClusterNode> it = event.topologyNodes().iterator();
            int matches = 0;
            while (it.hasNext()) {
                final String igniteName = it.next().attribute(ATTR_IGNITE_NAME);
                if (nodes.contains(igniteName)) {
                    ++matches;
                }
            }
            if (matches == nodes.size()) {
                logger.info("[TopologyVerifier] {}: Topology completed!", ignName);
                s.release(1);
            } else {
                logger.info("[TopologyVerifier] {}: Uncompleted topology ({}/{})", ignName, matches, nodes.size());
            }
            return true;
        }

    }

    // InitializingBean

    /**
     * After properties set.
     *
     * @throws Exception
     *     the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // nothing to do
    }

    // DisposableBean

    /**
     * Destroy.
     *
     * @throws Exception
     *     the exception
     */
    @Override
    public void destroy() throws Exception {
        if (ignite != null && Ignition.state(ignite.name()) == IgniteState.STARTED) {
            ignite.close();
        }
    }

}
