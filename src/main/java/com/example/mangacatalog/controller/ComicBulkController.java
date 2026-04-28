package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.BulkComicRequest;
import com.example.mangacatalog.dto.BulkComicResult;
import com.example.mangacatalog.service.ComicBulkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comics/bulk")
@Tag(name = "Bulk-операции", description = "Массовое создание комиксов")
public class ComicBulkController {

    private final ComicBulkService bulkService;

    public ComicBulkController(ComicBulkService bulkService) {
        this.bulkService = bulkService;
    }

    @PostMapping
    @Operation(
        summary = "Массовое создание комиксов (единая транзакция)",
        description = """
                    Создаёт список комиксов в рамках ОДНОЙ транзакции (@Transactional).
                    
                    Если хотя бы один элемент невалиден — ВСЯ операция откатывается,
                    ни один комикс не будет сохранён в БД.
                    
                    Для демонстрации разницы: если убрать @Transactional с метода сервиса,
                    каждый save() будет фиксироваться немедленно — при ошибке на N-м элементе
                    предыдущие (0..N-1) уже останутся в БД.
                    """
    )
    public ResponseEntity<BulkComicResult> createBulk(
        @Valid @RequestBody BulkComicRequest request) {
        BulkComicResult result = bulkService.createBulk(request.comics());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}