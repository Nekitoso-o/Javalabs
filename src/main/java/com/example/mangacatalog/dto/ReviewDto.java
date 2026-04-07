package com.example.mangacatalog.dto;

public record ReviewDto(
    Long id,
    String text,
    Integer rating,
    Long comicId
) {

}