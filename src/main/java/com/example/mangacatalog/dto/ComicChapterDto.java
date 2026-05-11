package com.example.mangacatalog.dto;

import java.util.List;

public record ComicChapterDto(
    Long id,
    Double chapterNumber,
    String title,
    int pageCount,
    String createdAt,
    List<ChapterPageDto> pages
) {}