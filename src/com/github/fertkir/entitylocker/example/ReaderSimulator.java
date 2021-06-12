package com.github.fertkir.entitylocker.example;

import java.util.Random;

import static com.github.fertkir.entitylocker.example.BookRepository.BOOK_NAMES;

public class ReaderSimulator implements Runnable {

    private final LibraryService libraryService;
    private final String username;
    private final Random random = new Random();

    public ReaderSimulator(LibraryService libraryService, String username) {
        this.libraryService = libraryService;
        this.username = username;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String book = getRandomBook();
            if (libraryService.tryTakeBook(username, book)) {
                readBook();
                libraryService.returnBook(username, book);
            }
        }
    }

    private String getRandomBook() {
        return BOOK_NAMES.get(random.nextInt(BOOK_NAMES.size()));
    }

    private void readBook() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
