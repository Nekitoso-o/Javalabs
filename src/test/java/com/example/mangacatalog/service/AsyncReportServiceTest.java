package com.example.mangacatalog.service;

import com.example.mangacatalog.repository.AuthorRepository;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private AsyncReportService asyncReportService;

    private static final String TASK_ID = "test-task-123";

    @BeforeEach
    void setUp() {
        asyncReportService.initTask(TASK_ID);
    }



    @Test
    void processReportAsync_success() throws Exception {
        when(comicRepository.count()).thenReturn(10L);
        when(reviewRepository.count()).thenReturn(25L);
        when(authorRepository.count()).thenReturn(5L);

        CompletableFuture<String> future =
            asyncReportService.processReportAsync(TASK_ID);

        String result = future.get();

        assertTrue(result.contains("Комиксов - 10"));
        assertTrue(result.contains("Отзывов - 25"));
        assertTrue(result.contains("Авторов - 5"));

        assertEquals("Завершено успешно", asyncReportService.getStatus(TASK_ID));
        assertEquals(result, asyncReportService.getResult(TASK_ID));
    }



    @Test
    void processReportAsync_exceptionDuringCount_setsErrorStatus() {
        when(comicRepository.count()).thenThrow(new RuntimeException("DB error"));

        CompletableFuture<String> future =
            asyncReportService.processReportAsync(TASK_ID);

        assertTrue(future.isCompletedExceptionally());

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertTrue(ex.getCause() instanceof RuntimeException);
        assertEquals("DB error", ex.getCause().getMessage());

        assertEquals("Ошибка", asyncReportService.getStatus(TASK_ID));
    }



    @Test
    void initTask_setsStatusInProcess() {
        String newTaskId = "another-task";
        asyncReportService.initTask(newTaskId);
        assertEquals("В процессе", asyncReportService.getStatus(newTaskId));
    }



    @Test
    void getStatus_unknownTask_returnsDefault() {
        assertEquals("Задача не найдена",
            asyncReportService.getStatus("non-existent-id"));
    }

    @Test
    void getResult_unknownTask_returnsDefault() {
        assertEquals("Результат еще не готов",
            asyncReportService.getResult("non-existent-id"));
    }


}