
package com.example.mangacatalog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyServiceTest {

    private ConcurrencyService concurrencyService;

    @BeforeEach
    void setUp() {
        concurrencyService = new ConcurrencyService();
    }



    @Test
    @DisplayName("Начальные значения всех счётчиков равны 0")
    void initialValues_allZero() {
        assertEquals(0, concurrencyService.getUnsafeCounter());
        assertEquals(0, concurrencyService.getSyncCounter());
        assertEquals(0, concurrencyService.getAtomicCounter());
    }



    @Test
    @DisplayName("incrementUnsafe — однопоточный инкремент работает корректно")
    void incrementUnsafe_singleThread() {
        concurrencyService.incrementUnsafe();
        concurrencyService.incrementUnsafe();
        concurrencyService.incrementUnsafe();

        assertEquals(3, concurrencyService.getUnsafeCounter());
    }

    @Test
    @DisplayName("incrementUnsafe — одиночный вызов увеличивает счётчик на 1")
    void incrementUnsafe_single_increasesBy1() {
        concurrencyService.incrementUnsafe();

        assertEquals(1, concurrencyService.getUnsafeCounter());
    }



    @Test
    @DisplayName("incrementSync — однопоточный инкремент работает корректно")
    void incrementSync_singleThread() {
        concurrencyService.incrementSync();
        concurrencyService.incrementSync();
        concurrencyService.incrementSync();

        assertEquals(3, concurrencyService.getSyncCounter());
    }

    @Test
    @DisplayName("incrementSync — многопоточный инкремент даёт точный результат")
    void incrementSync_multiThread_exactResult()
        throws InterruptedException {
        int threads = 10;
        int incrementsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    concurrencyService.incrementSync();
                }
                latch.countDown();
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threads * incrementsPerThread,
            concurrencyService.getSyncCounter());
    }

    @Test
    @DisplayName("incrementSync — одиночный вызов увеличивает счётчик на 1")
    void incrementSync_single_increasesBy1() {
        concurrencyService.incrementSync();

        assertEquals(1, concurrencyService.getSyncCounter());
    }



    @Test
    @DisplayName("incrementAtomic — однопоточный инкремент работает корректно")
    void incrementAtomic_singleThread() {
        concurrencyService.incrementAtomic();
        concurrencyService.incrementAtomic();
        concurrencyService.incrementAtomic();

        assertEquals(3, concurrencyService.getAtomicCounter());
    }

    @Test
    @DisplayName("incrementAtomic — многопоточный инкремент даёт точный результат")
    void incrementAtomic_multiThread_exactResult()
        throws InterruptedException {
        int threads = 10;
        int incrementsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    concurrencyService.incrementAtomic();
                }
                latch.countDown();
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threads * incrementsPerThread,
            concurrencyService.getAtomicCounter());
    }

    @Test
    @DisplayName("incrementAtomic — одиночный вызов увеличивает счётчик на 1")
    void incrementAtomic_single_increasesBy1() {
        concurrencyService.incrementAtomic();

        assertEquals(1, concurrencyService.getAtomicCounter());
    }



    @Test
    @DisplayName("resetCounters — все счётчики сбрасываются в 0")
    void resetCounters_allZero() {
        concurrencyService.incrementUnsafe();
        concurrencyService.incrementSync();
        concurrencyService.incrementAtomic();

        concurrencyService.resetCounters();

        assertEquals(0, concurrencyService.getUnsafeCounter());
        assertEquals(0, concurrencyService.getSyncCounter());
        assertEquals(0, concurrencyService.getAtomicCounter());
    }

    @Test
    @DisplayName("resetCounters — сброс не влияет на другие счётчики после сброса")
    void resetCounters_afterReset_canIncrementAgain() {
        concurrencyService.incrementUnsafe();
        concurrencyService.incrementSync();
        concurrencyService.incrementAtomic();
        concurrencyService.resetCounters();

        concurrencyService.incrementUnsafe();
        concurrencyService.incrementSync();
        concurrencyService.incrementAtomic();

        assertEquals(1, concurrencyService.getUnsafeCounter());
        assertEquals(1, concurrencyService.getSyncCounter());
        assertEquals(1, concurrencyService.getAtomicCounter());
    }

    @Test
    @DisplayName("resetCounters — повторный сброс не вызывает ошибок")
    void resetCounters_doubleReset_noError() {
        concurrencyService.resetCounters();
        concurrencyService.resetCounters();

        assertEquals(0, concurrencyService.getUnsafeCounter());
        assertEquals(0, concurrencyService.getSyncCounter());
        assertEquals(0, concurrencyService.getAtomicCounter());
    }



    @Test
    @DisplayName("Счётчики независимы друг от друга")
    void counters_areIndependent() {
        concurrencyService.incrementUnsafe();
        concurrencyService.incrementUnsafe();
        concurrencyService.incrementSync();
        concurrencyService.incrementAtomic();
        concurrencyService.incrementAtomic();
        concurrencyService.incrementAtomic();

        assertEquals(2, concurrencyService.getUnsafeCounter());
        assertEquals(1, concurrencyService.getSyncCounter());
        assertEquals(3, concurrencyService.getAtomicCounter());
    }

    @RepeatedTest(3)
    @DisplayName("incrementSync и incrementAtomic — многопоточная согласованность (repeated)")
    void syncAndAtomic_multiThread_consistent() throws InterruptedException {
        concurrencyService.resetCounters();
        int threads = 5;
        int increments = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads * 2);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < increments; j++) {
                    concurrencyService.incrementSync();
                }
                latch.countDown();
            });
            executor.submit(() -> {
                for (int j = 0; j < increments; j++) {
                    concurrencyService.incrementAtomic();
                }
                latch.countDown();
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threads * increments, concurrencyService.getSyncCounter());
        assertEquals(threads * increments, concurrencyService.getAtomicCounter());
    }
}