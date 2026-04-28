package com.example.mangacatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Информация об ошибке при bulk-операции")
public record BulkErrorItem(
    @Schema(description = "Индекс элемента в списке (0-based)")
    int index,

    @Schema(description = "Название комикса, при создании которого возникла ошибка")
    String title,

    @Schema(description = "Описание ошибки")
    String error
) {}