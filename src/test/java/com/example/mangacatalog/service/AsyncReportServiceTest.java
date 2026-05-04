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
        service = new AsyncReportService(
            comicRepository, reviewRepository, authorRepository
        );

        ReflectionTestUtils.setField(service, "simulationDelayMs", 0);
        service.initTask(TASK_ID);
    }


    @Test
    void processReportAsync_success() throws Exception {
        when(comicRepository.count()).thenReturn(10L);
        when(reviewRepository.count()).thenReturn(25L);
        when(authorRepository.count()).thenReturn(5L);

        String result = service.processReportAsync(TASK_ID).get();

        assertTrue(result.contains("Комиксов - 10"));
        assertTrue(result.contains("Отзывов - 25"));
        assertTrue(result.contains("Авторов - 5"));
        assertEquals("Завершено успешно", service.getStatus(TASK_ID));
        assertEquals(result, service.getResult(TASK_ID));
    }


    @Test
    void processReportAsync_interrupted_setsInterruptedStatus()
        throws Exception {


        ReflectionTestUtils.setField(service, "simulationDelayMs", 5000);

        Thread[] workerThread = new Thread[1];
        CompletableFuture<String>[] future = new CompletableFuture[1];

        Thread runner = new Thread(() -> {
            workerThread[0] = Thread.currentThread();
            future[0] = service.processReportAsync(TASK_ID);
        });
        runner.start();

        Thread.sleep(100);

        runner.interrupt();

        runner.join(2000);

        assertEquals("Прервано", service.getStatus(TASK_ID));
    }


    @Test
    void processReportAsync_exception_setsErrorStatus() {
        when(comicRepository.count())
            .thenThrow(new RuntimeException("DB недоступна"));

        CompletableFuture<String> future =
            service.processReportAsync(TASK_ID);

        assertTrue(future.isCompletedExceptionally());
        assertEquals("Ошибка", service.getStatus(TASK_ID));

        ExecutionException ex = assertThrows(
            ExecutionException.class, future::get
        );
        assertEquals("DB недоступна", ex.getCause().getMessage());
    }

    @Test
    void getStatus_unknownTask_returnsDefault() {
        assertEquals("Задача не найдена", service.getStatus("unknown"));
    }

    @Test
    void getResult_unknownTask_returnsDefault() {
        assertEquals("Результат еще не готов", service.getResult("unknown"));
    }


    @Test
    void initTask_setsInProgressStatus() {
        service.initTask("new-task");
        assertEquals("В процессе", service.getStatus("new-task"));
    }
}