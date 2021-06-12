package com.github.fertkir.entitylocker;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class ReentrantLockEntityLocker<ID> implements EntityLocker<ID> {

    private final Map<ID, Lock> entityLocks = new ConcurrentHashMap<>();
    private final Lock globalLock;

    public ReentrantLockEntityLocker() {
        this.globalLock = createLock();
    }

    private Lock createLock() {
        return new ReentrantLock();
    }

    @Override
    public void lock(Runnable callback) {
        lock(() -> {
            callback.run();
            return null;
        });
    }

    @Override
    public <T> T lock(Supplier<T> callback) {
        globalLock.lock();
        try {
            return callback.get();
        } finally {
            globalLock.unlock();
        }
    }

    @Override
    public void tryLock(Duration timeout, Runnable callback) throws InterruptedException, TimeoutException {
        tryLock(timeout, () -> {
            callback.run();
            return null;
        });
    }

    @Override
    public <T> T tryLock(Duration timeout, Supplier<T> callback) throws InterruptedException, TimeoutException {
        if (globalLock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            try {
                return callback.get();
            } finally {
                globalLock.unlock();
            }
        } else {
            throw new TimeoutException();
        }
    }

    @Override
    public void lock(ID id, Runnable callback) {
        lock(id, () -> {
            callback.run();
            return null;
        });
    }

    @Override
    public <T> T lock(ID id, Supplier<T> callback) {
        Lock lock = entityLocks.computeIfAbsent(id, ignored -> createLock());
        lock.lock();
        try {
            return callback.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void tryLock(ID id, Duration timeout, Runnable callback) throws InterruptedException, TimeoutException {
        tryLock(id, timeout, () -> {
            callback.run();
            return null;
        });
    }

    @Override
    public <T> T tryLock(ID id, Duration timeout, Supplier<T> callback) throws InterruptedException, TimeoutException {
        Lock lock = entityLocks.computeIfAbsent(id, ignored -> createLock());
        if (lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            try {
                return callback.get();
            } finally {
                lock.unlock();
            }
        } else {
            throw new TimeoutException();
        }
    }
}
