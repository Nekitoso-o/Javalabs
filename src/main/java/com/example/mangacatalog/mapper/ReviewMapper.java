package com.example.mangacatalog.mapper;
import com.example.mangacatalog.dto.ReviewDto;
import com.example.mangacatalog.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    public ReviewDto toDto(Review entity) {
        if (entity == null) return null;
        ReviewDto dto = new ReviewDto();
        dto.setId(entity.getId());
        dto.setText(entity.getText());
        dto.setRating(entity.getRating());
        if (entity.getComic() != null) {
            dto.setComicId(entity.getComic().getId());
        }
        return dto;
    }
}