package com.github.fertkir.entitylocker.example;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class BookRepository {
    public static final List<String> BOOK_NAMES = asList(
            "Ulysses",
            "Don Quixote",
            "One Hundred Years of Solitude"
    );

    private final Map<String, Book> books = BOOK_NAMES
            .stream()
            .map(Book::new)
            .collect(Collectors.toMap(Book::getName, Function.identity()));

    public Book getByName(String name) {
        return books.get(name);
    }
}
