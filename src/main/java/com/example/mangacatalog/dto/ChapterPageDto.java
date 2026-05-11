package com.example.mangacatalog.dto;

public record ChapterPageDto(
    Long id,
    String url,
    String originalName,
    Integer sortOrder
) {}