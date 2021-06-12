package com.github.fertkir.entitylocker.example;

import com.github.fertkir.entitylocker.EntityLocker;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class LibraryService {
    private final BookRepository bookRepository;
    private final EntityLocker<String> locker;

    public LibraryService(BookRepository bookRepository, EntityLocker<String> locker) {
        this.bookRepository = bookRepository;
        this.locker = locker;
    }

    public void returnBook(String username, String bookName) {
        locker.lock(bookName, () -> {
            Book book = bookRepository.getByName(bookName);
            String currentReader = book.getCurrentReader();
            if (username.equals(currentReader) && book.isBeingRead()) {
                book.setBeingRead(false);
                book.dequeueFirst();
                System.out.printf("%s returned \"%s\", queue: %s%n", username, bookName, book.getQueueState());
            } else {
                throw new RuntimeException(String.format("\"%s\" is not being read by %s", bookName, username));
            }
        });
    }

    public boolean tryTakeBook(String username, String bookName) {
        try {
            return locker.tryLock(bookName, Duration.ofSeconds(1), () -> {
                Book book = bookRepository.getByName(bookName);
                String currentReader = book.getCurrentReader();
                if (currentReader == null || username.equals(currentReader)) {
                    if (book.isBeingRead()) {
                        throw new RuntimeException(String.format("\"%s\" is already being read by %s", bookName, username));
                    } else {
                        book.setBeingRead(true);
                        if (currentReader == null) {
                            book.enqueue(username);
                        }
                        System.out.printf("%s took \"%s\", queue: %s%n", username, bookName, book.getQueueState());
                        return true;
                    }
                } else {
                    if (!book.isReaderInQueue(username)) {
                        book.enqueue(username);
                        System.out.printf("%s entered the queue for \"%s\", queue: %s%n", username, bookName, book.getQueueState());
                    }
                    return false;
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
