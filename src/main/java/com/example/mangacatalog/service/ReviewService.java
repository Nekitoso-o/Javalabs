package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.dto.ReviewDto;
import com.example.mangacatalog.dto.ReviewPatchRequest;
import com.example.mangacatalog.dto.ReviewRequest;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Review;
import com.example.mangacatalog.mapper.ReviewMapper;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewService.class);

    private static final String REVIEW_NOT_FOUND_MSG = "Отзыв с ID %s не найден!";
    private static final String COMIC_NOT_FOUND_MSG = "Комикс с ID %s не найден!";
    private static final String CACHE_HIT_MSG = "Кэш ХИТ Отзывы: {}";
    private static final String CACHE_MISS_MSG = "Кэш МИСС Отзывы. Запрос к БД";

    private final ReviewRepository reviewRepository;
    private final ComicRepository comicRepository;
    private final ReviewMapper reviewMapper;
    private final Map<ApiCacheKey, Object> cache = new ConcurrentHashMap<>();

    public ReviewService(ReviewRepository reviewRepository,
                         ComicRepository comicRepository, ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.comicRepository = comicRepository;
        this.reviewMapper = reviewMapper;
    }

    private void invalidateCache() {
        LOG.info("Инвалидация: Очистка In-Memory кеша Отзывов.");
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    public List<ReviewDto> getAllReviews() {
        ApiCacheKey key = new ApiCacheKey("getAllReviews");
        if (cache.containsKey(key)) {
            LOG.info(CACHE_HIT_MSG, key);
            return (List<ReviewDto>) cache.get(key);
        }
        LOG.info(CACHE_MISS_MSG);
        List<ReviewDto> result = reviewRepository.findAll().stream().map(reviewMapper::toDto).toList();
        cache.put(key, result);
        return result;
    }

    public ReviewDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getReviewById", id);
        if (cache.containsKey(key)) {
            LOG.info(CACHE_HIT_MSG, key);
            return (ReviewDto) cache.get(key);
        }
        LOG.info("Кэш МИСС Отзывы. Запрос к БД для ID: {}", id);
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(REVIEW_NOT_FOUND_MSG, id)));
        ReviewDto result = reviewMapper.toDto(review);
        cache.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ReviewDto> getReviewsByComicId(Long comicId) {
        ApiCacheKey key = new ApiCacheKey("getReviewsByComicId", comicId);
        if (cache.containsKey(key)) {
            LOG.info(CACHE_HIT_MSG, key);
            return (List<ReviewDto>) cache.get(key);
        }
        LOG.info("Кэш МИСС Отзывы. Запрос к БД для Comic ID: {}", comicId);
        List<ReviewDto> result = reviewRepository.findByComicId(comicId).stream().map(reviewMapper::toDto).toList();
        cache.put(key, result);
        return result;
    }

    @Transactional
    public ReviewDto addReviewToComic(ReviewRequest request) {
        Comic comic = comicRepository.findById(request.comicId())
            .orElseThrow(() -> new ResourceNotFoundException(String.format(COMIC_NOT_FOUND_MSG, request.comicId())));
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
            .orElseThrow(() -> new ResourceNotFoundException(String.format(REVIEW_NOT_FOUND_MSG, id)));
        existing.setText(request.text());
        existing.setRating(request.rating());

        ReviewDto result = reviewMapper.toDto(reviewRepository.save(existing));
        invalidateCache();
        return result;
    }

    @Transactional
    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException(String.format(REVIEW_NOT_FOUND_MSG, id));
        }
        reviewRepository.deleteById(id);
        invalidateCache();
    }

    @Transactional
    public ReviewDto patch(Long id, ReviewPatchRequest request) {
        Review existing = reviewRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(REVIEW_NOT_FOUND_MSG, id)));

        if (request.text() != null) {
            existing.setText(request.text());
        }

        if (request.rating() != null) {
            existing.setRating(request.rating());
        }

        if (request.comicId() != null) {
            Comic comic = comicRepository.findById(request.comicId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COMIC_NOT_FOUND_MSG, request.comicId())));
            existing.setComic(comic);
        }

        ReviewDto result = reviewMapper.toDto(reviewRepository.save(existing));
        invalidateCache();
        return result;
    }
}