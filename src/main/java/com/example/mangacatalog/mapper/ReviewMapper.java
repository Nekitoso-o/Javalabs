package com.example.mangacatalog.mapper;

import com.example.mangacatalog.dto.ReviewDto;
import com.example.mangacatalog.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    public ReviewDto toDto(Review entity) {
        if (entity == null) return null;
        Long comicId = entity.getComic() != null ? entity.getComic().getId() : null;
        return new ReviewDto(entity.getId(), entity.getText(), entity.getRating(), comicId);
    }
}