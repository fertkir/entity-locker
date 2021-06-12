package com.github.fertkir.entitylocker.example;

import java.util.Deque;
import java.util.LinkedList;

public class Book {
    private final String name;
    private boolean isBeingRead = false;
    private final Deque<String> readersQueue = new LinkedList<>();

    public Book(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isReaderInQueue(String reader) {
        return readersQueue.contains(reader);
    }

    public String getCurrentReader() {
        return readersQueue.peekFirst();
    }

    public void enqueue(String reader) {
        readersQueue.addLast(reader);
    }

    public void dequeueFirst() {
        readersQueue.removeFirst();
    }

    public String getQueueState() {
        return readersQueue.toString();
    }

    public boolean isBeingRead() {
        return isBeingRead;
    }

    public void setBeingRead(boolean beingRead) {
        isBeingRead = beingRead;
    }
}
