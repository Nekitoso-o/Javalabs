package com.example.mangacatalog.service;
import com.example.mangacatalog.dto.ReviewDto;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Review;
import com.example.mangacatalog.mapper.ReviewMapper;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ComicRepository comicRepository;
    private final ReviewMapper reviewMapper;

    @Transactional
    public ReviewDto addReviewToComic(ReviewDto dto) {
        Comic comic = comicRepository.findById(dto.getComicId())
            .orElseThrow(() -> new RuntimeException("Comic not found"));

        Review review = new Review();
        review.setText(dto.getText());
        review.setRating(dto.getRating());
        review.setComic(comic);

        return reviewMapper.toDto(reviewRepository.save(review));
    }

    public List<ReviewDto> getAllReviews() {
        return reviewRepository.findAll().stream()
            .map(reviewMapper::toDto)
            .toList();
    }

    public List<ReviewDto> getReviewsByComicId(Long comicId) {
        return reviewRepository.findByComicId(comicId).stream()
            .map(reviewMapper::toDto)
            .toList();
    }
}