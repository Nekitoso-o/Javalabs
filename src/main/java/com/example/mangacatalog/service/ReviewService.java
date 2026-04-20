package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.cache.ApiCacheManager;
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

@Service
public class ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewService.class);
    private static final String REVIEW_NOT_FOUND_MSG = "Отзыв с ID %s не найден!";
    private static final String COMIC_NOT_FOUND_MSG = "Комикс с ID %s не найден!";

    private final ReviewRepository reviewRepository;
    private final ComicRepository comicRepository;
    private final ReviewMapper reviewMapper;
    private final ApiCacheManager cacheManager;

    public ReviewService(ReviewRepository reviewRepository,
                         ComicRepository comicRepository,
                         ReviewMapper reviewMapper,
                         ApiCacheManager cacheManager) {
        this.reviewRepository = reviewRepository;
        this.comicRepository = comicRepository;
        this.reviewMapper = reviewMapper;
        this.cacheManager = cacheManager;
    }

    @SuppressWarnings("unchecked")
    public List<ReviewDto> getAllReviews() {
        ApiCacheKey key = new ApiCacheKey("getAllReviews");

        Object cached = cacheManager.get(key);
        if (cached != null) return (List<ReviewDto>) cached;

        List<ReviewDto> result = reviewRepository.findAll().stream().map(reviewMapper::toDto).toList();
        cacheManager.put(key, result);
        return result;
    }

    public ReviewDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getReviewById", id);

        Object cached = cacheManager.get(key);
        if (cached != null) return (ReviewDto) cached;

        LOG.info("Запрос к БД для ID: {}", id);
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(REVIEW_NOT_FOUND_MSG, id)));

        ReviewDto result = reviewMapper.toDto(review);
        cacheManager.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ReviewDto> getReviewsByComicId(Long comicId) {
        ApiCacheKey key = new ApiCacheKey("getReviewsByComicId", comicId);

        Object cached = cacheManager.get(key);
        if (cached != null) return (List<ReviewDto>) cached;

        LOG.info("Запрос к БД для Comic ID: {}", comicId);
        List<ReviewDto> result = reviewRepository.findByComicId(comicId).stream().map(reviewMapper::toDto).toList();

        cacheManager.put(key, result);
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
        cacheManager.invalidate();
        return result;
    }

    @Transactional
    public ReviewDto update(Long id, ReviewRequest request) {
        Review existing = reviewRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(REVIEW_NOT_FOUND_MSG, id)));

        existing.setText(request.text());
        existing.setRating(request.rating());

        ReviewDto result = reviewMapper.toDto(reviewRepository.save(existing));
        cacheManager.invalidate();
        return result;
    }

    @Transactional
    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException(String.format(REVIEW_NOT_FOUND_MSG, id));
        }
        reviewRepository.deleteById(id);
        cacheManager.invalidate();
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
        cacheManager.invalidate();
        return result;
    }
}