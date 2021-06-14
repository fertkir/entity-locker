package com.github.fertkir.entitylocker;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.*;
import java.util.function.Supplier;

public class EntityLockerImpl<ID> implements EntityLocker<ID> {

    private static final int DEFAULT_ESCALATION_THRESHOLD = 10;

    /**
     * todo We never delete values from this map, which may cause unrestricted memory consumption.
     * todo DelayQueue can be engaged to implement clean-up mechanism.
     */
    private final Map<ID, ReentrantLock> entityLocks = new ConcurrentHashMap<>();

    private final StampedLock globalLock = new StampedLock();
    private final ThreadLocal<Integer> lockedEntitiesCount = ThreadLocal.withInitial(() -> 0);
    private final ThreadLocal<Integer> readLockReentranceCount = ThreadLocal.withInitial(() -> 0);
    private final ThreadLocal<Long> readLockStamp = ThreadLocal.withInitial(() -> 0L);
    private final int escalationThreshold;

    public EntityLockerImpl() {
        this(DEFAULT_ESCALATION_THRESHOLD);
    }

    public EntityLockerImpl(int escalationThreshold) {
        this.escalationThreshold = escalationThreshold;
    }

    @Override
    public void lockGlobally(Runnable callback) {
        lockGlobally(() -> {
            callback.run();
            return null;
        });
    }

    @Override
    public <T> T lockGlobally(Supplier<T> callback) {
        long stamp = globalLock.writeLock();
        try {
            return callback.get();
        } finally {
            globalLock.unlock(stamp);
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
        long stamp = globalLock.tryWriteLock(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (stamp != 0) {
            try {
                return callback.get();
            } finally {
                globalLock.unlock(stamp);
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
        try {
            return tryLockInternal(id, callback, lock -> {
                lock.lock(); return true;
            });
        } catch (InterruptedException | TimeoutException e) {
            throw new AssertionError("lock.lock() should not have thrown this exception", e);
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
        return tryLockInternal(id, callback, lock ->
                lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)
        );
    }

    private <T> T tryLockInternal(
            ID id,
            Supplier<T> callback,
            TryLockCallback tryLockCallback
    ) throws InterruptedException, TimeoutException {
        if (readLockReentranceCount.get() == 0) {
            readLockStamp.set(globalLock.readLock());
        }
        readLockReentranceCount.set(readLockReentranceCount.get() + 1);
        if (lockedEntitiesCount.get() >= escalationThreshold) {
            long escalatedStamp = globalLock.tryConvertToWriteLock(readLockStamp.get());
            if (escalatedStamp != 0) {
                readLockStamp.set(escalatedStamp);
            }
        }
        try {
            ReentrantLock entityLock = getEntityLock(id);
            if (tryLockCallback.tryLock(entityLock)) {
                if (entityLock.getHoldCount() == 1) {
                    lockedEntitiesCount.set(lockedEntitiesCount.get() + 1);
                }
                try {
                    return callback.get();
                } finally {
                    if (entityLock.getHoldCount() == 1) {
                        lockedEntitiesCount.set(lockedEntitiesCount.get() - 1);
                    }
                    entityLock.unlock();
                }
            } else {
                throw new TimeoutException();
            }
        } finally {
            readLockReentranceCount.set(readLockReentranceCount.get() - 1);
            if (readLockReentranceCount.get() == 0) {
                globalLock.unlock(readLockStamp.get());
            }
        }
    }

    private ReentrantLock getEntityLock(ID id) {
        return entityLocks.computeIfAbsent(id, ignored -> new ReentrantLock());
    }

    @FunctionalInterface
    private interface TryLockCallback {
        boolean tryLock(Lock lock) throws InterruptedException, TimeoutException;
    }
}
