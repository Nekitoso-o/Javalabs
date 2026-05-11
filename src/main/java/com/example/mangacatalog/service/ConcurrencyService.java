package com.example.mangacatalog.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ConcurrencyService {

    private volatile int unsafeCounter;
    private int syncCounter;
    private final AtomicInteger atomicCounter = new AtomicInteger(0);


    public void incrementUnsafe() {
        unsafeCounter++;
    }


    public synchronized void incrementSync() {
        syncCounter++;
    }


    public void incrementAtomic() {
        atomicCounter.incrementAndGet();
    }

    public int getUnsafeCounter()  { return unsafeCounter; }
    public int getSyncCounter()    { return syncCounter; }
    public int getAtomicCounter()  { return atomicCounter.get(); }

    public void resetCounters() {
        unsafeCounter = 0;
        syncCounter   = 0;
        atomicCounter.set(0);
    }
}