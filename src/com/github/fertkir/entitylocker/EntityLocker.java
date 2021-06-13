package com.github.fertkir.entitylocker;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public interface EntityLocker<ID> {
    void lockGlobally(Runnable callback);
    <T> T lockGlobally(Supplier<T> callback);
    void tryLockGlobally(Duration timeout, Runnable callback) throws InterruptedException, TimeoutException;
    <T> T tryLockGlobally(Duration timeout, Supplier<T> callback) throws InterruptedException, TimeoutException;

    void lock(ID id, Runnable callback);
    <T> T lock(ID id, Supplier<T> callback);
    void tryLock(ID id, Duration timeout, Runnable callback) throws InterruptedException, TimeoutException;
    <T> T tryLock(ID id, Duration timeout, Supplier<T> callback) throws InterruptedException, TimeoutException;
}
