package com.example.mangacatalog.controller;

import com.example.mangacatalog.service.AsyncReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Асинхронные отчёты", description = "Демонстрация @Async / CompletableFuture")
public class AsyncReportController {

    private static final String TASK_ID_KEY = "taskId";

    private final AsyncReportService asyncReportService;

    public AsyncReportController(final AsyncReportService asyncReportService) {
        this.asyncReportService = asyncReportService;
    }

    @PostMapping("/start")
    @Operation(summary = "Запустить генерацию отчёта")
    public ResponseEntity<Map<String, String>> startReport() {
        String taskId = UUID.randomUUID().toString();
        asyncReportService.initTask(taskId);
        asyncReportService.processReportAsync(taskId);
        return ResponseEntity.accepted().body(Map.of(
            TASK_ID_KEY, taskId,
            "message", "Отчёт генерируется. Проверьте /api/reports/status/" + taskId
        ));
    }

    @GetMapping("/status/{taskId}")
    @Operation(summary = "Статус задачи")
    public ResponseEntity<Map<String, String>> getStatus(
        @PathVariable final String taskId) {
        return ResponseEntity.ok(Map.of(
            TASK_ID_KEY, taskId,
            "status", asyncReportService.getStatus(taskId)
        ));
    }

    @GetMapping("/result/{taskId}")
    @Operation(summary = "Результат задачи")
    public ResponseEntity<Map<String, String>> getResult(
        @PathVariable final String taskId) {
        return ResponseEntity.ok(Map.of(
            TASK_ID_KEY, taskId,
            "result", asyncReportService.getResult(taskId)
        ));
    }
}