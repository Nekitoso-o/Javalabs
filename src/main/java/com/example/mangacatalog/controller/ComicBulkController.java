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
            Создаёт список комиксов. Каждый элемент сохраняется в отдельной транзакции.
            
            Ошибка в одном элементе НЕ откатывает уже сохранённые комиксы.
            
            Возвращает:
            - 201 Created — все элементы сохранены успешно
            - 207 Multi-Status — часть сохранена, часть с ошибками
            - 400 Bad Request — ни один не сохранён (все с ошибками)
            
            Для демонстрации: передайте список где один элемент
            содержит несуществующий authorId — остальные сохранятся в БД.
            """
    )
    public ResponseEntity<BulkComicResult> createBulk(
        @Valid @RequestBody BulkComicRequest request) {

        BulkComicResult result = bulkService.createBulk(request.comics());

        HttpStatus status;
        if (result.errors().isEmpty()) {
            status = HttpStatus.CREATED;
        } else if (!result.created().isEmpty()) {
            status = HttpStatus.MULTI_STATUS;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        return ResponseEntity.status(status).body(result);
    }
}