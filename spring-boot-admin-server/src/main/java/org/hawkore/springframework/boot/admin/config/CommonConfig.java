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
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import de.codecentric.boot.admin.server.domain.values.Registration;
import de.codecentric.boot.admin.server.utils.jackson.RegistrationDeserializer;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.hawkore.springframework.boot.admin.cluster.IgniteCacheConcurrentMapWrapper;
import org.hawkore.springframework.boot.admin.cluster.IgniteEventStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private static final String SPRING_BOOT_ADMIN_SERVER_EVENT_STORE = "SPRING_BOOT_ADMIN_SERVER_EVENT_STORE";

    /**
     * Creates an event store for spring boot admin server in cluster
     *
     * @param igniteConnectionManager
     *     the ignite connection manager
     * @param maxLogSizePerAggregate
     *     the max log size per aggregate, default 100
     * @return the ignite event store
     * @See de.codecentric.boot.admin.server.config.AdminServerAutoConfiguration
     */
    @Bean
    public IgniteEventStore eventStore(@Autowired Ignite ignite,
        @Value("${spring.boot.admin.server.max_events_per_aggregate:100}") int maxLogSizePerAggregate) {

        CacheConfiguration<InstanceId, List<InstanceEvent>> config = new CacheConfiguration<>();
        config.setName(SPRING_BOOT_ADMIN_SERVER_EVENT_STORE);
        config.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        config.setCacheMode(CacheMode.REPLICATED);
        return new IgniteEventStore(maxLogSizePerAggregate,
            new IgniteCacheConcurrentMapWrapper<InstanceId, List<InstanceEvent>>(ignite, config));
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
