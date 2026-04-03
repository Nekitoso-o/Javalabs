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

    public List<ReviewDto> getAllReviews() {
        return reviewRepository.findAll().stream().map(reviewMapper::toDto).toList();
    }

    public ReviewDto getById(Long id) {
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Review with ID " + id + " not found!"));
        return reviewMapper.toDto(review);
    }

    public List<ReviewDto> getReviewsByComicId(Long comicId) {
        return reviewRepository.findByComicId(comicId).stream().map(reviewMapper::toDto).toList();
    }

    @Transactional
    public ReviewDto addReviewToComic(ReviewDto dto) {
        Comic comic = comicRepository.findById(dto.getComicId())
            .orElseThrow(() -> new IllegalArgumentException("Comic not found!"));

        Review review = new Review();
        review.setText(dto.getText());
        review.setRating(dto.getRating());
        review.setComic(comic);

        return reviewMapper.toDto(reviewRepository.save(review));
    }

    @Transactional
    public ReviewDto update(Long id, ReviewDto dto) {
        Review existing = reviewRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Review with ID " + id + " not found!"));

        existing.setText(dto.getText());
        existing.setRating(dto.getRating());

        return reviewMapper.toDto(reviewRepository.save(existing));
    }

    @Transactional
    public ReviewDto patch(Long id, ReviewDto dto) {
        Review existing = reviewRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Review with ID " + id + " not found!"));

        if (dto.getText() != null) {
            existing.setText(dto.getText());
        }
        if (dto.getRating() != null) {
            existing.setRating(dto.getRating());
        }

        return reviewMapper.toDto(reviewRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new IllegalArgumentException("Review with ID " + id + " not found!");
        }
        reviewRepository.deleteById(id);
    }
}