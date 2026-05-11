package com.example.mangacatalog.dto;

public record ComicImageDto(
    Long id,
    String url,
    String originalName,
    Integer sortOrder
) {}