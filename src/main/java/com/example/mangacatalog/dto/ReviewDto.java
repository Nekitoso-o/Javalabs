package com.example.mangacatalog.dto;
import lombok.Data;

@Data
public class ReviewDto {
    private Long id;
    private String text;
    private Integer rating;
    private Long comicId;
}