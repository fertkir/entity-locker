package com.github.fertkir.entitylocker;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public interface EntityLocker<ID> {
    void lock(Runnable callback);
    <T> T lock(Supplier<T> callback);
    void tryLock(Duration timeout, Runnable callback) throws InterruptedException, TimeoutException;
    <T> T tryLock(Duration timeout, Supplier<T> callback) throws InterruptedException, TimeoutException;

    void lock(ID id, Runnable callback);
    <T> T lock(ID id, Supplier<T> callback);
    void tryLock(ID id, Duration timeout, Runnable callback) throws InterruptedException, TimeoutException;
    <T> T tryLock(ID id, Duration timeout, Supplier<T> callback) throws InterruptedException, TimeoutException;
}
