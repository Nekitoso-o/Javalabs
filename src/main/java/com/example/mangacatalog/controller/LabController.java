package com.example.mangacatalog.controller;

import com.example.mangacatalog.service.AsyncReportService;
import com.example.mangacatalog.service.ConcurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/lab")
@Tag(name = "Асинхронность и Многопоточность")
public class LabController {

    private static final String TASK_ID_KEY      = "ID Задачи";
    private static final int    THREAD_COUNT     = 50;
    private static final int    ITERATION_COUNT  = 10_000;
    private static final int    AWAIT_TIMEOUT_SECONDS = 30;

    private final AsyncReportService asyncService;
    private final ConcurrencyService concurrencyService;

    public LabController(final AsyncReportService asyncService,
                         final ConcurrencyService concurrencyService) {
        this.asyncService      = asyncService;
        this.concurrencyService = concurrencyService;
    }


    @PostMapping("/async/start")
    @Operation(summary = "Запустить генерацию отчёта по БД")
    public Map<String, String> startAsync() {
        String taskId = UUID.randomUUID().toString();
        asyncService.initTask(taskId);
        asyncService.processReportAsync(taskId);
        return Map.of(
            TASK_ID_KEY, taskId,
            "Сообщение", "Отчёт генерируется в фоне. Проверьте статус через 15 секунд."
        );
    }

    @GetMapping("/async/status/{id}")
    @Operation(summary = "Проверить статус задачи")
    public Map<String, String> checkStatus(@PathVariable final String id) {
        return Map.of(
            TASK_ID_KEY, id,
            "Статус", asyncService.getStatus(id)
        );
    }

    @GetMapping("/async/result/{id}")
    @Operation(summary = "Получить готовый отчёт")
    public Map<String, String> getResult(@PathVariable final String id) {
        return Map.of(
            TASK_ID_KEY, id,
            "Результат", asyncService.getResult(id)
        );
    }


    @PostMapping("/concurrency/test")
    @Operation(
        summary = "Демонстрация Race Condition (50 потоков × 10 000 итераций)",
        description = """
            Запускает 3 НЕЗАВИСИМЫХ теста в отдельных пулах потоков.
            Каждый счётчик тестируется изолированно — результаты точные.

            1. unsafeCounter  → покажет МЕНЬШЕ 500 000 (Race Condition).
            2. synchronized   → ровно 500 000 (блокировка метода).
            3. AtomicInteger  → ровно 500 000 (lock-free CAS операция).

            ⚠️ Блокирует HTTP-поток ~5-10 секунд.
            """
    )
    public Map<String, Object> testConcurrency() throws InterruptedException {
        concurrencyService.resetCounters();
        int expected = THREAD_COUNT * ITERATION_COUNT;

        runIsolatedTest(concurrencyService::incrementUnsafe);
        int unsafeResult = concurrencyService.getUnsafeCounter();


        concurrencyService.resetCounters();
        runIsolatedTest(concurrencyService::incrementSync);
        int syncResult = concurrencyService.getSyncCounter();


        concurrencyService.resetCounters();
        runIsolatedTest(concurrencyService::incrementAtomic);
        int atomicResult = concurrencyService.getAtomicCounter();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("Ожидаемое значение (потоков × итераций)",
            THREAD_COUNT + " × " + ITERATION_COUNT + " = " + expected);

        result.put("1. Небезопасный счётчик (Race Condition!)", unsafeResult);
        result.put("   -> ПОТЕРЯНО ДАННЫХ", expected - unsafeResult);

        result.put("2. Безопасный (synchronized)", syncResult);

        result.put("3. Безопасный (AtomicInteger)", atomicResult);

        return result;
    }


    private void runIsolatedTest(Runnable action) throws InterruptedException {
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneLatch   = new CountDownLatch(THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startSignal.await();
                    for (int j = 0; j < ITERATION_COUNT; j++) {
                        action.run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startSignal.countDown();
        boolean finished = doneLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        executor.shutdown();

        if (!finished) {
            executor.shutdownNow();
        }
    }
}