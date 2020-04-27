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
package org.hawkore.springframework.boot.admin.config;

import java.util.List;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.values.Registration;
import de.codecentric.boot.admin.server.notify.NotificationTrigger;
import de.codecentric.boot.admin.server.notify.Notifier;
import de.codecentric.boot.admin.server.utils.jackson.RegistrationDeserializer;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.hawkore.springframework.boot.admin.cluster.IgniteEventStore;
import org.hawkore.springframework.boot.admin.cluster.IgniteNotificationTrigger;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Common config.
 *
 * @author Manuel Núñez Sánchez (manuel.nunez@hawkore.com)
 */
@Configuration
public class CommonConfig {

    private static final String DEFAULT_NAME_EVENT_STORE_CACHE = "spring-boot-admin-event-store";
    @Value("${spring.boot.admin.ignite.event-store:" + DEFAULT_NAME_EVENT_STORE_CACHE + "}")
    private String nameEventStore;
    private static final String DEFAULT_NAME_SENT_NOTIFICATIONS_CACHE = "spring-boot-admin-sent-notifications";
    @Value("${spring.boot.admin.ignite.notification-store:" + DEFAULT_NAME_SENT_NOTIFICATIONS_CACHE + "}")
    private String nameNotificationStore;

    /**
     * Creates an event store for Spring Boot Admin server in cluster
     *
     * @param ignite
     *     the ignite instance
     * @param maxLogSizePerAggregate
     *     the max log size per aggregate, default 100
     * @return the ignite event store
     * @See de.codecentric.boot.admin.server.config.AdminServerAutoConfiguration
     */
    @Bean
    public IgniteEventStore eventStore(@Autowired Ignite ignite,
        @Value("${spring.boot.admin.server.max_events_per_aggregate:100}") int maxLogSizePerAggregate) {
        CacheConfiguration<String, List<InstanceEvent>> config = new CacheConfiguration<>();
        config.setName(nameEventStore);
        config.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        config.setCacheMode(CacheMode.REPLICATED);
        return new IgniteEventStore(maxLogSizePerAggregate, ignite.getOrCreateCache(config));
    }

    /**
     * Creates a Notification trigger for Spring Boot Admin server in cluster
     *
     * @param ignite
     *     the ignite
     * @param notifier
     *     the notifier
     * @param events
     *     the events
     * @return the notification trigger
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean(NotificationTrigger.class)
    public NotificationTrigger notificationTrigger(@Autowired Ignite ignite,
        @Autowired Notifier notifier,
        @Autowired Publisher<InstanceEvent> events) {
        CacheConfiguration<String, Long> config = new CacheConfiguration<>();
        config.setName(nameNotificationStore);
        config.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        config.setCacheMode(CacheMode.REPLICATED);
        // we dont want to preserve old notifications for a long time...
        // so set an expiry policy to auto remove them after a time
        config.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
        return new IgniteNotificationTrigger(notifier, events, ignite.getOrCreateCache(config));
    }

    /**
     * Object mapper object mapper.
     *
     * @return the object mapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Register deserializer for Registration class into current ObjectMapper for Spring Boot Admin
     *
     * @param objectMapper
     *     the current object mapper
     * @return the object mapper
     */
    @Bean
    @Primary
    public ObjectMapper jacksonObjectMapper(@Autowired ObjectMapper objectMapper) {
        SimpleModule simpleModule = new SimpleModule("SimpleModule", Version.unknownVersion());
        simpleModule.addDeserializer(Registration.class, new RegistrationDeserializer());
        objectMapper.registerModule(simpleModule);
        return objectMapper;
    }

}
