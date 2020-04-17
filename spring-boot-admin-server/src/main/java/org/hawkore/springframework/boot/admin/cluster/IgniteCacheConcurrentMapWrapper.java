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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * IgniteCacheConcurrentMapWrapper
 * <p>
 * This is not a ConcurrentMap implementation, just to access underline IgniteCache from IgniteEventStore
 *
 * @param <K>
 *     the key type
 * @param <V>
 *     the value type
 * @author Manuel Núñez Sánchez (manuel.nunez@hawkore.com)
 */
public class IgniteCacheConcurrentMapWrapper<K, V> implements ConcurrentMap<K, V> {

    private final IgniteCache<K, V> delegate;
    private final Ignite ignite;
    private final CacheConfiguration<K, V> config;

    /**
     * Instantiates a new Ignite cache wrapper serializable.
     *
     * @param igniteSupplier
     *     the ignite supplier
     * @param config
     *     the config
     */
    public IgniteCacheConcurrentMapWrapper(Ignite ignite, CacheConfiguration<K, V> config) {
        this.ignite = ignite;
        this.config = config;
        this.delegate = this.ignite.getOrCreateCache(this.config);
    }

    /**
     * Gets delegate.
     *
     * @return the delegate
     */
    public IgniteCache<K, V> getDelegate() {
        return delegate;
    }

    /**
     * Gets ignite supplier.
     *
     * @return the ignite supplier
     */
    public Ignite getIgnite() {
        return ignite;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return delegate.getName();
    }

    /**
     * Is closed boolean.
     *
     * @return the boolean
     */
    public boolean isClosed() {
        return delegate.isClosed();
    }

    /**
     * Lock lock.
     *
     * @param key
     *     the key
     * @return the lock
     */
    public Lock lock(K key) {
        return delegate.lock(key);
    }

    /**
     * Size int.
     *
     * @return the int
     */
    @Override
    public int size() {
        return delegate.size();
    }

    /**
     * Is empty boolean.
     *
     * @return the boolean
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Contains key boolean.
     *
     * @param key
     *     the key
     * @return the boolean
     */
    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    /**
     * Contains value boolean.
     *
     * @param value
     *     the value
     * @return the boolean
     */
    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get v.
     *
     * @param key
     *     the key
     * @return the v
     */
    @Override
    public V get(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Put v.
     *
     * @param key
     *     the key
     * @param value
     *     the value
     * @return the v
     */
    @Nullable
    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove v.
     *
     * @param key
     *     the key
     * @return the v
     */
    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Put all.
     *
     * @param m
     *     the m
     */
    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Key set set.
     *
     * @return the set
     */
    @NotNull
    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Values collection.
     *
     * @return the collection
     */
    @NotNull
    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    /**
     * Entry set set.
     *
     * @return the set
     */
    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Put if absent v.
     *
     * @param key
     *     the key
     * @param value
     *     the value
     * @return the v
     */
    @Override
    public V putIfAbsent(@NotNull K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove boolean.
     *
     * @param key
     *     the key
     * @param value
     *     the value
     * @return the boolean
     */
    @Override
    public boolean remove(@NotNull Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replace boolean.
     *
     * @param key
     *     the key
     * @param oldValue
     *     the old value
     * @param newValue
     *     the new value
     * @return the boolean
     */
    @Override
    public boolean replace(@NotNull K key, @NotNull V oldValue, @NotNull V newValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replace v.
     *
     * @param key
     *     the key
     * @param value
     *     the value
     * @return the v
     */
    @Override
    public V replace(@NotNull K key, @NotNull V value) {
        throw new UnsupportedOperationException();
    }

}
