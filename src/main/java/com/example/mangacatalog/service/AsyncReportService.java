package com.example.mangacatalog.service;

import com.example.mangacatalog.repository.AuthorRepository;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AsyncReportService {

    private static final Logger LOG =
        LoggerFactory.getLogger(AsyncReportService.class);

    private final Map<String, String> taskStatuses = new ConcurrentHashMap<>();
    private final Map<String, String> taskResults  = new ConcurrentHashMap<>();

    private final ComicRepository comicRepository;
    private final ReviewRepository reviewRepository;
    private final AuthorRepository authorRepository;

    // Берётся из application.properties, по умолчанию 10000 мс
    @Value("${report.simulation.delay-ms:10000}")
    private int simulationDelayMs;

    // Единственный конструктор — Spring autowiring работает корректно
    public AsyncReportService(ComicRepository comicRepository,
                              ReviewRepository reviewRepository,
                              AuthorRepository authorRepository) {
        this.comicRepository  = comicRepository;
        this.reviewRepository = reviewRepository;
        this.authorRepository = authorRepository;
    }

    public void initTask(final String taskId) {
        taskStatuses.put(taskId, "В процессе");
    }

    @Async
    public CompletableFuture<String> processReportAsync(final String taskId) {
        LOG.info("Сбор аналитики начался в фоне (Task ID: {})", taskId);
        try {
            Thread.sleep(simulationDelayMs);

            long comicsCount  = comicRepository.count();
            long reviewsCount = reviewRepository.count();
            long authorsCount = authorRepository.count();

            String result = String.format(
                "Отчет готов! В базе данных найдено: Комиксов - %d, Отзывов - %d, Авторов - %d",
                comicsCount, reviewsCount, authorsCount
            );

            taskResults.put(taskId, result);
            taskStatuses.put(taskId, "Завершено успешно");
            LOG.info("Отчет сформирован (Task ID: {})", taskId);

            return CompletableFuture.completedFuture(result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            taskStatuses.put(taskId, "Прервано");
            LOG.warn("Задача {} была прервана", taskId);
            return CompletableFuture.failedFuture(e);

        } catch (Exception e) {
            taskStatuses.put(taskId, "Ошибка");
            LOG.error("Ошибка при генерации отчёта {}: {}", taskId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public String getStatus(final String taskId) {
        return taskStatuses.getOrDefault(taskId, "Задача не найдена");
    }

    public String getResult(final String taskId) {
        return taskResults.getOrDefault(taskId, "Результат еще не готов");
    }
}