package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.dto.ReviewDto;
import com.example.mangacatalog.dto.ReviewRequest;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Review;
import com.example.mangacatalog.mapper.ReviewMapper;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ComicRepository comicRepository;
    private final ReviewMapper reviewMapper;

    private final Map<ApiCacheKey, Object> cache = new ConcurrentHashMap<>();

    private void invalidateCache() {
        log.info(" Инвалидация: Очистка In-Memory кеша Отзывов.");
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    public List<ReviewDto> getAllReviews() {
        ApiCacheKey key = new ApiCacheKey("getAllReviews");
        if (cache.containsKey(key)) {
            log.info(" Кэш ХИТ Отзывы: {}", key);
            return (List<ReviewDto>) cache.get(key);
        }
        log.info(" Кэш МИСС Отзывы. Запрос к БД");
        List<ReviewDto> result = reviewRepository.findAll().stream().map(reviewMapper::toDto).toList();
        cache.put(key, result);
        return result;
    }

    public ReviewDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getReviewById", id);
        if (cache.containsKey(key)) {
            log.info(" Кэш ХИТ Отзывы: {}", key);
            return (ReviewDto) cache.get(key);
        }
        log.info(" Кэш МИСС Отзывы. Запрос к БД для ID: {}", id);
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Отзыв с ID " + id + " не найден!"));
        ReviewDto result = reviewMapper.toDto(review);
        cache.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ReviewDto> getReviewsByComicId(Long comicId) {
        ApiCacheKey key = new ApiCacheKey("getReviewsByComicId", comicId);
        if (cache.containsKey(key)) {
            log.info(" Кэш ХИТ Отзывы: {}", key);
            return (List<ReviewDto>) cache.get(key);
        }
        log.info(" Кэш МИСС Отзывы. Запрос к БД для Comic ID: {}", comicId);
        List<ReviewDto> result = reviewRepository.findByComicId(comicId).stream().map(reviewMapper::toDto).toList();
        cache.put(key, result);
        return result;
    }

    @Transactional
    public ReviewDto addReviewToComic(ReviewRequest request) {
        Comic comic = comicRepository.findById(request.comicId())
            .orElseThrow(() -> new ResourceNotFoundException("Комикс с ID " + request.comicId() + " не найден!"));

        Review review = new Review();
        review.setText(request.text());
        review.setRating(request.rating());
        review.setComic(comic);

        ReviewDto result = reviewMapper.toDto(reviewRepository.save(review));
        invalidateCache();
        return result;
    }

    @Transactional
    public ReviewDto update(Long id, ReviewRequest request) {
        Review existing = reviewRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Отзыв с ID " + id + " не найден!"));

        existing.setText(request.text());
        existing.setRating(request.rating());

        ReviewDto result = reviewMapper.toDto(reviewRepository.save(existing));
        invalidateCache();
        return result;
    }

    @Transactional
    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Отзыв с ID " + id + " не найден!");
        }
        reviewRepository.deleteById(id);
        invalidateCache();
    }
}