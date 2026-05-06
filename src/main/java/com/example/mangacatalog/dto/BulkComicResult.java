package com.example.mangacatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Результат массового создания комиксов")
public record BulkComicResult(
    @Schema(description = "Успешно созданные комиксы")
    List<ComicDto> created,

    @Schema(description = "Количество созданных комиксов")
    int successCount
) {}