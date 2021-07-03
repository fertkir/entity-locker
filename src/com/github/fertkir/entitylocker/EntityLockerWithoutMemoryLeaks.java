package com.github.fertkir.entitylocker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class EntityLockerWithoutMemoryLeaks<ID> {

    private final Map<ID, Locker> entityLocks = new ConcurrentHashMap<>();

    public void lock(ID id, Runnable callback) {
        lock(id, () -> {
            callback.run();
            return null;
        });
    }

    public <T> T lock(ID id, Supplier<T> callback) {
        Locker entityLock = entityLocks.compute(id, (k, v) -> {
            if (v == null) {
                Locker locker = new Locker();
                locker.incWaitingCount();
                return locker;
            } else {
                v.incWaitingCount();
                return v;
            }
        });
        entityLock.lock();
        try {
            return callback.get();
        } finally {
            entityLock.unlock();
            entityLocks.computeIfPresent(id, (k, v) -> {
                if (v.decWaitingCount() <= 0) {
                    return null;
                }
                return v;
            });
        }
    }

    private static class Locker {
        private final ReentrantLock reentrantLock = new ReentrantLock();
        private int waitingCount = 0;

        public void incWaitingCount() {
            ++waitingCount;
        }

        public int decWaitingCount() {
            return --waitingCount;
        }

        public void lock() {
            reentrantLock.lock();
        }

        public void unlock() {
            reentrantLock.unlock();
        }
    }
}
