package com.github.fertkir.entitylocker;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class ReentrantLockEntityLocker<ID> implements EntityLocker<ID> {

    private final Map<ID, Lock> entityLocks = new ConcurrentHashMap<>();
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();

    @Override
    public void lockGlobally(Runnable callback) {
        lockGlobally(() -> {
            callback.run();
            return null;
        });
    }

    @Override
    public <T> T lockGlobally(Supplier<T> callback) {
        Lock lock = globalLock.writeLock();
        lock.lock();
        try {
            return callback.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void tryLockGlobally(Duration timeout, Runnable callback) throws InterruptedException, TimeoutException {
        tryLockGlobally(timeout, () -> {
            callback.run();
            return null;
        });
    }

    @Override
    public <T> T tryLockGlobally(Duration timeout, Supplier<T> callback) throws InterruptedException, TimeoutException {
        Lock lock = globalLock.writeLock();
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

    @Override
    public void lock(ID id, Runnable callback) {
        lock(id, () -> {
            callback.run();
            return null;
        });
    }

    @Override
    public <T> T lock(ID id, Supplier<T> callback) {
        Lock readLock = globalLock.readLock();
        readLock.lock();
        try {
            Lock lock = getLock(id);
            lock.lock();
            try {
                return callback.get();
            } finally {
                lock.unlock();
            }
        } finally {
            readLock.unlock();
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
        Lock readLock = globalLock.readLock();
        readLock.lock();
        try {
            Lock lock = getLock(id);
            if (lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                try {
                    return callback.get();
                } finally {
                    lock.unlock();
                }
            } else {
                throw new TimeoutException();
            }
        } finally {
            readLock.unlock();
        }
    }

    private Lock getLock(ID id) {
        return entityLocks.computeIfAbsent(id, ignored -> new ReentrantLock());
    }
}
