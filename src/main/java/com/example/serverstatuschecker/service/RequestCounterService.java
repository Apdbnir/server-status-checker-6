package com.example.serverstatuschecker.service;

import com.example.serverstatuschecker.counter.RequestCounter;
import org.springframework.stereotype.Service;

@Service
public class RequestCounterService {

    private final RequestCounter counter;

    public RequestCounterService() {
        this.counter = new RequestCounter();
    }

    public void increment() {
        counter.increment();
    }

    public long getCount() {
        return counter.getCount();
    }

    public void reset() {
        counter.reset();
    }
}