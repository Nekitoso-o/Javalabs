package com.example.mangacatalog.dto;

import lombok.Data;

@Data
public class ComicDto {
    private Long id;
    private String title;
    private String author;
    private String genre;
}