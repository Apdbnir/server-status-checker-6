package com.example.serverstatuschecker.counter;

import java.util.concurrent.atomic.AtomicLong;

public class RequestCounter {
    private final AtomicLong count = new AtomicLong(0);

    public void increment() {
        count.incrementAndGet();
    }

    public long getCount() {
        return count.get();
    }

    public void reset() {
        count.set(0);
    }
}