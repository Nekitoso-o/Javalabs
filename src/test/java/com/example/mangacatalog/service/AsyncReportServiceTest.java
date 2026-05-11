package com.example.mangacatalog.service;

import com.example.mangacatalog.repository.AuthorRepository;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncReportServiceTest {

    @Mock private ComicRepository  comicRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private AuthorRepository authorRepository;

    private AsyncReportService service;

    private static final String TASK_ID = "test-task";

    @BeforeEach
    void setUp() {
        service = new AsyncReportService(comicRepository, reviewRepository, authorRepository);
        // Убираем задержку для быстрого выполнения тестов
        ReflectionTestUtils.setField(service, "simulationDelayMs", 0);
        service.initTask(TASK_ID);
    }

    // ─────────────────────────── initTask ───────────────────────────

    @Test
    void initTask_setsInProgressStatus() {
        service.initTask("new-task");
        assertEquals("В процессе", service.getStatus("new-task"));
    }

    @Test
    void initTask_overwritesExistingStatus() {
        service.initTask(TASK_ID);
        // Статус должен снова стать "В процессе"
        assertEquals("В процессе", service.getStatus(TASK_ID));
    }

    // ─────────────────────── processReportAsync ──────────────────────

    @Test
    void processReportAsync_success_returnsFormattedReport() throws Exception {
        when(comicRepository.count()).thenReturn(10L);
        when(reviewRepository.count()).thenReturn(25L);
        when(authorRepository.count()).thenReturn(5L);

        String result = service.processReportAsync(TASK_ID).get();

        assertTrue(result.contains("Комиксов - 10"),
            "Отчёт должен содержать количество комиксов");
        assertTrue(result.contains("Отзывов - 25"),
            "Отчёт должен содержать количество отзывов");
        assertTrue(result.contains("Авторов - 5"),
            "Отчёт должен содержать количество авторов");
    }

    @Test
    void processReportAsync_success_setsCompletedStatus() throws Exception {
        when(comicRepository.count()).thenReturn(1L);
        when(reviewRepository.count()).thenReturn(1L);
        when(authorRepository.count()).thenReturn(1L);

        service.processReportAsync(TASK_ID).get();

        assertEquals("Завершено успешно", service.getStatus(TASK_ID));
    }

    @Test
    void processReportAsync_success_savesResultForRetrieval() throws Exception {
        when(comicRepository.count()).thenReturn(3L);
        when(reviewRepository.count()).thenReturn(7L);
        when(authorRepository.count()).thenReturn(2L);

        String result = service.processReportAsync(TASK_ID).get();

        assertEquals(result, service.getResult(TASK_ID),
            "getResult должен вернуть тот же текст, что и CompletableFuture");
    }

    @Test
    void processReportAsync_exception_setsErrorStatus() {
        when(comicRepository.count())
            .thenThrow(new RuntimeException("DB недоступна"));

        CompletableFuture<String> future = service.processReportAsync(TASK_ID);

        assertTrue(future.isCompletedExceptionally(),
            "Future должен завершиться исключительно");
        assertEquals("Ошибка", service.getStatus(TASK_ID));
    }

    @Test
    void processReportAsync_exception_futureCauseMatchesOriginal() {
        when(comicRepository.count())
            .thenThrow(new RuntimeException("DB недоступна"));

        CompletableFuture<String> future = service.processReportAsync(TASK_ID);

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertEquals("DB недоступна", ex.getCause().getMessage());
    }

    @Test
    void processReportAsync_interrupted_setsInterruptedStatus() throws Exception {
        // Устанавливаем задержку, чтобы успеть прервать поток
        ReflectionTestUtils.setField(service, "simulationDelayMs", 5_000);

        AtomicReference<CompletableFuture<String>> futureRef = new AtomicReference<>();
        AtomicReference<Thread> workerRef = new AtomicReference<>();

        Thread runner = new Thread(() -> {
            workerRef.set(Thread.currentThread());
            futureRef.set(service.processReportAsync(TASK_ID));
        });
        runner.start();

        // Ждём, пока поток войдёт в sleep
        Thread.sleep(200);
        runner.interrupt();
        runner.join(3_000);

        assertEquals("Прервано", service.getStatus(TASK_ID));
    }

    // ─────────────────────────── getStatus ───────────────────────────

    @Test
    void getStatus_unknownTask_returnsDefault() {
        assertEquals("Задача не найдена", service.getStatus("unknown-id"));
    }

    @Test
    void getStatus_knownTask_returnsCurrentStatus() {
        assertEquals("В процессе", service.getStatus(TASK_ID));
    }

    // ─────────────────────────── getResult ───────────────────────────

    @Test
    void getResult_unknownTask_returnsDefault() {
        assertEquals("Результат еще не готов", service.getResult("unknown-id"));
    }

    @Test
    void getResult_beforeCompletion_returnsNotReadyMessage() {
        // Задача инициализирована, но processReportAsync не вызывался
        assertEquals("Результат еще не готов", service.getResult(TASK_ID));
    }
}