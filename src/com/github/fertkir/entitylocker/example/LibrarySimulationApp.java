package com.github.fertkir.entitylocker.example;

import com.github.fertkir.entitylocker.ReentrantLockEntityLocker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class LibrarySimulationApp {

    private static final int AMOUNT_OF_READERS = 10;

    public static void main(String[] args) {
        LibraryService libraryService = new LibraryService(
                new BookRepository(),
                new ReentrantLockEntityLocker<>()
        );
        ExecutorService executorService = Executors.newFixedThreadPool(AMOUNT_OF_READERS);
        IntStream.range(0, AMOUNT_OF_READERS).forEach(value ->
                executorService.submit(new ReaderSimulator(libraryService, String.valueOf(value + 1)))
        );
    }
}
