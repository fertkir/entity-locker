package com.github.fertkir.entitylocker;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class SynchronizedEntityLocker<ID> implements EntityLocker<ID> {

    private final Map<ID, Object> entityLocks = new ConcurrentHashMap<>();
    private final Object globalLock = new Object();

    @Override
    public void lock(Runnable callback) {
        synchronized (globalLock) {
            callback.run();
        }
    }

    @Override
    public <T> T lock(Supplier<T> callback) {
        synchronized (globalLock) {
            return callback.get();
        }
    }

    @Override
    public void tryLock(Duration timeout, Runnable callback) throws InterruptedException, TimeoutException {
        throw new UnsupportedOperationException(); // todo implement
    }

    @Override
    public <T> T tryLock(Duration timeout, Supplier<T> callback) throws InterruptedException, TimeoutException {
        throw new UnsupportedOperationException(); // todo implement
    }

    @Override
    public void lock(ID id, Runnable callback) {
        synchronized (entityLocks.computeIfAbsent(id, ignored -> new Object())) {
            callback.run();
        }
    }

    @Override
    public <T> T lock(ID id, Supplier<T> callback) {
        synchronized (entityLocks.computeIfAbsent(id, ignored -> new Object())) {
            return callback.get();
        }
    }

    @Override
    public void tryLock(ID id, Duration timeout, Runnable callback) throws InterruptedException, TimeoutException {
        throw new UnsupportedOperationException(); // todo implement
    }

    @Override
    public <T> T tryLock(ID id, Duration timeout, Supplier<T> callback) throws InterruptedException, TimeoutException {
        throw new UnsupportedOperationException(); // todo implement
    }
}
