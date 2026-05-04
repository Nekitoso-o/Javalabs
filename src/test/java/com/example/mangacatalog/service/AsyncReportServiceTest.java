package com.example.mangacatalog.service;

import com.example.mangacatalog.repository.AuthorRepository;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncReportServiceTest {

    @Mock
    private ComicRepository comicRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private AuthorRepository authorRepository;

    private AsyncReportService asyncReportService;

    @BeforeEach
    void setUp() {
        asyncReportService = new AsyncReportService(
            comicRepository, reviewRepository, authorRepository);
    }



    @Test
    @DisplayName("initTask — статус устанавливается 'В процессе'")
    void initTask_setsStatusInProgress() {
        asyncReportService.initTask("task-1");

        assertEquals("В процессе", asyncReportService.getStatus("task-1"));
    }

    @Test
    @DisplayName("initTask — несколько задач не пересекаются")
    void initTask_multipleTasks_independent() {
        asyncReportService.initTask("task-A");
        asyncReportService.initTask("task-B");

        assertEquals("В процессе", asyncReportService.getStatus("task-A"));
        assertEquals("В процессе", asyncReportService.getStatus("task-B"));
    }



    @Test
    @DisplayName("getStatus — задача не найдена — возвращает дефолтное сообщение")
    void getStatus_taskNotFound_returnsDefault() {
        String status = asyncReportService.getStatus("non-existent-task");

        assertEquals("Задача не найдена", status);
    }

    @Test
    @DisplayName("getStatus — после initTask — возвращает 'В процессе'")
    void getStatus_afterInit_returnsInProgress() {
        asyncReportService.initTask("task-2");

        assertEquals("В процессе", asyncReportService.getStatus("task-2"));
    }



    @Test
    @DisplayName("getResult — результат ещё не готов — возвращает дефолтное сообщение")
    void getResult_notReady_returnsDefault() {
        String result = asyncReportService.getResult("task-3");

        assertEquals("Результат еще не готов", result);
    }

    @Test
    @DisplayName("getResult — несуществующая задача — возвращает дефолтное сообщение")
    void getResult_nonExistentTask_returnsDefault() {
        String result = asyncReportService.getResult("unknown-task");

        assertEquals("Результат еще не готов", result);
    }



    @Test
    @DisplayName("processReportAsync — успешно завершается, статус 'Завершено успешно'")
    void processReportAsync_success() throws ExecutionException, InterruptedException {
        when(comicRepository.count()).thenReturn(10L);
        when(reviewRepository.count()).thenReturn(5L);
        when(authorRepository.count()).thenReturn(3L);

        AsyncReportService fastService = new AsyncReportService(
            comicRepository, reviewRepository, authorRepository) {
        };

        String taskId = "task-fast";
        fastService.initTask(taskId);

        CompletableFuture<String> future = fastService.processReportAsync(taskId);

        String reportResult = future.get();

        assertNotNull(reportResult);
        assertTrue(reportResult.contains("10"));
        assertTrue(reportResult.contains("5"));
        assertTrue(reportResult.contains("3"));
        assertEquals("Завершено успешно", fastService.getStatus(taskId));
        assertEquals(reportResult, fastService.getResult(taskId));
    }

    @Test
    @DisplayName("processReportAsync — результат содержит корректные данные из репозиториев")
    void processReportAsync_resultContainsCorrectCounts()
        throws ExecutionException, InterruptedException {
        when(comicRepository.count()).thenReturn(42L);
        when(reviewRepository.count()).thenReturn(100L);
        when(authorRepository.count()).thenReturn(7L);

        String taskId = "task-counts";
        asyncReportService.initTask(taskId);

        CompletableFuture<String> future =
            asyncReportService.processReportAsync(taskId);
        String result = future.get();

        assertTrue(result.contains("42"));
        assertTrue(result.contains("100"));
        assertTrue(result.contains("7"));
        assertTrue(result.contains("Отчет готов!"));
    }

    @Test
    @DisplayName("processReportAsync — статус меняется на 'Завершено успешно'")
    void processReportAsync_statusBecomesCompleted()
        throws ExecutionException, InterruptedException {
        when(comicRepository.count()).thenReturn(0L);
        when(reviewRepository.count()).thenReturn(0L);
        when(authorRepository.count()).thenReturn(0L);

        String taskId = "task-status";
        asyncReportService.initTask(taskId);
        assertEquals("В процессе", asyncReportService.getStatus(taskId));

        asyncReportService.processReportAsync(taskId).get();

        assertEquals("Завершено успешно", asyncReportService.getStatus(taskId));
    }

    @Test
    @DisplayName("processReportAsync — результат сохраняется и доступен через getResult")
    void processReportAsync_resultStoredAndAccessible()
        throws ExecutionException, InterruptedException {
        when(comicRepository.count()).thenReturn(1L);
        when(reviewRepository.count()).thenReturn(2L);
        when(authorRepository.count()).thenReturn(3L);

        String taskId = "task-result-stored";
        asyncReportService.initTask(taskId);

        String futureResult = asyncReportService.processReportAsync(taskId).get();
        String storedResult = asyncReportService.getResult(taskId);

        assertEquals(futureResult, storedResult);
        assertNotEquals("Результат еще не готов", storedResult);
    }

    @Test
    @DisplayName("processReportAsync — репозитории вызываются ровно по одному разу")
    void processReportAsync_repositoriesCalledOnce()
        throws ExecutionException, InterruptedException {
        when(comicRepository.count()).thenReturn(5L);
        when(reviewRepository.count()).thenReturn(5L);
        when(authorRepository.count()).thenReturn(5L);

        String taskId = "task-verify";
        asyncReportService.initTask(taskId);
        asyncReportService.processReportAsync(taskId).get();

        verify(comicRepository, times(1)).count();
        verify(reviewRepository, times(1)).count();
        verify(authorRepository, times(1)).count();
    }

    @Test
    @DisplayName("processReportAsync — несколько задач независимы")
    void processReportAsync_multipleTasks_independent()
        throws ExecutionException, InterruptedException {
        when(comicRepository.count()).thenReturn(10L);
        when(reviewRepository.count()).thenReturn(20L);
        when(authorRepository.count()).thenReturn(30L);

        asyncReportService.initTask("task-A");
        asyncReportService.initTask("task-B");

        asyncReportService.processReportAsync("task-A").get();
        asyncReportService.processReportAsync("task-B").get();

        assertEquals("Завершено успешно", asyncReportService.getStatus("task-A"));
        assertEquals("Завершено успешно", asyncReportService.getStatus("task-B"));
        assertNotNull(asyncReportService.getResult("task-A"));
        assertNotNull(asyncReportService.getResult("task-B"));
    }

    @Test
    @DisplayName("processReportAsync — нули в счётчиках — результат содержит нули")
    void processReportAsync_zeroCounts()
        throws ExecutionException, InterruptedException {
        when(comicRepository.count()).thenReturn(0L);
        when(reviewRepository.count()).thenReturn(0L);
        when(authorRepository.count()).thenReturn(0L);

        String taskId = "task-zeros";
        asyncReportService.initTask(taskId);

        String result = asyncReportService.processReportAsync(taskId).get();

        assertTrue(result.contains("0"));
        assertEquals("Завершено успешно", asyncReportService.getStatus(taskId));
    }
}