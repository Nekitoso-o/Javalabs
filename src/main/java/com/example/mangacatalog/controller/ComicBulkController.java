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
        summary = "Массовое создание комиксов",
        description = """
            Создаёт список комиксов. Демонстрирует роль @Transactional.
            
            С @Transactional на методе сервиса (текущее состояние):
              Все save() — в рамках ОДНОЙ транзакции.
              Ошибка на любом элементе → полный откат → БД не изменена.
            
            Без @Transactional (убрать аннотацию из ComicBulkService.createBulk):
              Каждый save() фиксируется немедленно.
              Ошибка на N-м элементе → элементы 0..N-1 уже в БД, откатить нельзя.
            
            Для демонстрации: передайте список где последний элемент
            содержит несуществующий authorId — и проверьте состояние БД.
            """
    )
    public ResponseEntity<BulkComicResult> createBulk(
        @Valid @RequestBody BulkComicRequest request) {
        BulkComicResult result = bulkService.createBulk(request.comics());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}