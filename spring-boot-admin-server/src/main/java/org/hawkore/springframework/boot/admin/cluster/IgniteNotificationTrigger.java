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

import java.util.Optional;

import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.notify.NotificationTrigger;
import de.codecentric.boot.admin.server.notify.Notifier;
import org.apache.ignite.IgniteCache;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Deduplicate notifications in cluster
 *
 * @author Manuel Núñez Sánchez (manuel.nunez@hawkore.com)
 */
public class IgniteNotificationTrigger extends NotificationTrigger {

    private static final Logger log = LoggerFactory.getLogger(IgniteNotificationTrigger.class);
    private final IgniteCache<String, Long> sentNotifications;

    public IgniteNotificationTrigger(Notifier notifier,
        Publisher<InstanceEvent> events,
        IgniteCache<String, Long> sentNotifications) {
        super(notifier, events);
        this.sentNotifications = sentNotifications;
    }

    @Override
    protected Mono<Void> sendNotifications(InstanceEvent event) {
        String instanceId = event.getInstance().getValue();
        try {
            while (true) {
                Long lastSentEvent = Optional.ofNullable(this.sentNotifications.get(instanceId)).orElse(-1L);
                if (lastSentEvent >= event.getVersion()) {
                    log.debug("Notifications already sent. Not triggering notifiers for {}", event);
                    return Mono.empty();
                }

                if (lastSentEvent < 0) {
                    if (this.sentNotifications.putIfAbsent(instanceId, event.getVersion())) {
                        log.debug("Triggering notifiers for {}", event);
                        return super.sendNotifications(event);
                    }
                } else {
                    if (this.sentNotifications.replace(instanceId, lastSentEvent, event.getVersion())) {
                        log.debug("Triggering notifiers for {}", event);
                        return super.sendNotifications(event);
                    }
                }
                Thread.sleep(10L);
            }
        } catch (InterruptedException e) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
        return Mono.empty();
    }

}
