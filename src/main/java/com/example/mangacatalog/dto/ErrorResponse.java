package com.example.mangacatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Стандартный ответ при ошибках (API Error)")
public record ErrorResponse(
    @Schema(description = "HTTP статус ошибки", example = "404")
    int status,

    @Schema(description = "Понятное сообщение", example = "Ресурс не найден")
    String message,

    @Schema(description = "Время возникновения ошибки")
    LocalDateTime timestamp,

    @Schema(description = "Список ошибок валидации (поле -> ошибка)")
    Map<String, String> validationErrors
) {
}