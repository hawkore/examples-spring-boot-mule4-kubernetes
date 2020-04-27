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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import de.codecentric.boot.admin.server.eventstore.InstanceEventPublisher;
import de.codecentric.boot.admin.server.eventstore.InstanceEventStore;
import de.codecentric.boot.admin.server.eventstore.OptimisticLockingException;
import javax.cache.Cache.Entry;
import javax.cache.event.EventType;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;

/**
 * Event-Store backed by a Apache IgniteCache for Spring Boot Admin storage over cluster
 * <p>
 * Just an adaptation of de.codecentric.boot.admin.server.eventstore.ConcurrentMapEventStore
 *
 * @author Manuel Núñez Sánchez (manuel.nunez@hawkore.com)
 */
public class IgniteEventStore extends InstanceEventPublisher implements InstanceEventStore, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(IgniteEventStore.class);
    private final QueryCursor<Entry<Serializable, Serializable>> continuousQueryCursor;
    private final int maxLogSizePerAggregate;
    private final IgniteCache<String, List<InstanceEvent>> eventLogCache;
    private static final Comparator<InstanceEvent> byTimestampAndIdAndVersion = comparing(InstanceEvent::getTimestamp)
                                                                                    .thenComparing(
                                                                                        InstanceEvent::getInstance)
                                                                                    .thenComparing(
                                                                                        InstanceEvent::getVersion);

    /**
     * Instantiates a new Ignite event store.
     *
     * @param eventLogs
     *     the event logs
     */
    public IgniteEventStore(IgniteCache<String, List<InstanceEvent>> eventLogCache) {
        this(100, eventLogCache);
    }

    /**
     * Instantiates a new Ignite event store.
     *
     * @param maxLogSizePerAggregate
     *     the max log size per aggregate
     * @param eventLogMap
     *     the event log
     */
    public IgniteEventStore(int maxLogSizePerAggregate, IgniteCache<String, List<InstanceEvent>> eventLogCache) {
        this.maxLogSizePerAggregate = maxLogSizePerAggregate;
        this.eventLogCache = eventLogCache;

        // create a monitor to update events on local instance from cluster udpates
        try {
            // Creating a continuous query.
            ContinuousQuery<Serializable, Serializable> qry = new ContinuousQuery<>();
            // Setting an optional initial query.
            qry.setInitialQuery(new ScanQuery<Serializable, Serializable>());
            // Local listener that is called locally when an update notification is received.
            qry.setLocalListener((evts) -> {
                evts.forEach(e -> {
                    if (e.getEventType().equals(EventType.UPDATED) || e.getEventType().equals(EventType.CREATED)) {
                        List<InstanceEvent> newEvents = (List<InstanceEvent>)e.getValue();
                        List<InstanceEvent> oldEvents = (List<InstanceEvent>)e.getOldValue();
                        long lastKnownVersion = oldEvents == null ? -1 : getLastVersion(oldEvents);
                        newEvents = newEvents.stream().filter(ev -> ev.getVersion() > lastKnownVersion)
                                        .collect(Collectors.toList());
                        if (!newEvents.isEmpty()) {
                            IgniteEventStore.this.publish(newEvents);
                        }
                    }
                });
            });
            // Executing the continuous query and preserve cursor without close it
            this.continuousQueryCursor = this.eventLogCache.query(qry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find all flux.
     *
     * @return the flux
     */
    @Override
    public Flux<InstanceEvent> findAll() {
        return Flux.defer(() -> {
            HashMap<String, List<InstanceEvent>> map = new HashMap<>();
            eventLogCache.forEach(e -> map.put(e.getKey(), e.getValue()));
            return Flux.fromIterable(map.values()).flatMapIterable(Function.identity())
                       .sort(byTimestampAndIdAndVersion);
        });
    }

    /**
     * Find flux.
     *
     * @param id
     *     the id
     * @return the flux
     */
    @Override
    public Flux<InstanceEvent> find(InstanceId id) {
        return Flux.defer(() -> {
            List<InstanceEvent> events = eventLogCache.get(id.getValue());
            if (events != null) {
                return Flux.fromIterable(events);
            }
            return Flux.fromIterable(Collections.emptyList());
        });
    }

    /**
     * Append mono.
     *
     * @param events
     *     the events
     * @return the mono
     */
    @Override
    public Mono<Void> append(List<InstanceEvent> events) {
        return Mono.fromRunnable(() -> doAppend(events));
    }

    private boolean doAppend(List<InstanceEvent> events) {
        if (events.isEmpty()) {
            return true;
        }

        String instanceId = events.get(0).getInstance().getValue();

        if (!events.stream().allMatch(event -> event.getInstance().getValue().equals(instanceId))) {
            throw new IllegalArgumentException("'events' must only refer to the same instance.");
        }

        // create a distributed lock per instance Id
        Lock lock = eventLogCache.lock(instanceId);

        try {
            lock.lock();

            List<InstanceEvent> oldEvents = Optional.ofNullable(eventLogCache.get(instanceId))
                                                .orElse(new ArrayList<>(maxLogSizePerAggregate + 1));

            long lastVersion = getLastVersion(oldEvents);

            if (lastVersion >= events.get(0).getVersion()) {
                throw createOptimisticLockException(events.get(0), lastVersion);
            }

            List<InstanceEvent> newEvents = new ArrayList<>(oldEvents);
            newEvents.addAll(events);

            if (newEvents.size() > maxLogSizePerAggregate) {
                if (log.isDebugEnabled()) {
                    log.debug("Threshold for {} reached. Compacting events", instanceId);
                }
                compact(newEvents);
            }

            if (oldEvents.equals(newEvents)) {
                if (log.isDebugEnabled()) {
                    log.debug("No new events to append {} ", events);
                }
                return false;
            }

            eventLogCache.put(instanceId, newEvents);

            if (log.isDebugEnabled()) {
                log.debug("Events appended to log {}", events);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    private void compact(List<InstanceEvent> events) {
        BinaryOperator<InstanceEvent> latestEvent = (e1, e2) -> e1.getVersion() > e2.getVersion() ? e1 : e2;
        Map<Class<?>, Optional<InstanceEvent>> latestPerType = events.stream().collect(
            groupingBy(InstanceEvent::getClass, reducing(latestEvent)));
        events.removeIf((e) -> !Objects.equals(e, latestPerType.get(e.getClass()).orElse(null)));
    }

    private OptimisticLockingException createOptimisticLockException(InstanceEvent event, long lastVersion) {
        return new OptimisticLockingException(
            "Verison " + event.getVersion() + " was overtaken by " + lastVersion + " for " + event.getInstance());
    }

    protected static long getLastVersion(List<InstanceEvent> events) {
        return events.isEmpty() ? -1 : events.get(events.size() - 1).getVersion();
    }

    /**
     * Destroy.
     *
     * @throws Exception
     *     the exception
     */
    @Override
    public void destroy() throws Exception {
        U.closeQuiet(this.continuousQueryCursor);
    }

}
