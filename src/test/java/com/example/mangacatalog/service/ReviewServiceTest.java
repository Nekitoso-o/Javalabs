package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.ReviewDto;
import com.example.mangacatalog.dto.ReviewPatchRequest;
import com.example.mangacatalog.dto.ReviewRequest;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Review;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.mapper.ReviewMapper;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ComicRepository comicRepository;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private ApiCacheManager cacheManager;

    @InjectMocks
    private ReviewService reviewService;

    private Review testReview;
    private ReviewDto testReviewDto;
    private Comic testComic;

    @BeforeEach
    void setUp() {
        testComic = new Comic();
        testComic.setId(1L);
        testComic.setTitle("One Piece");

        testReview = new Review();
        testReview.setId(1L);
        testReview.setText("Отличный комикс!");
        testReview.setRating(9);
        testReview.setComic(testComic);

        testReviewDto = new ReviewDto(1L, "Отличный комикс!", 9, 1L);
    }

    // ─── getAllReviews ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllReviews — из кэша")
    void getAllReviews_fromCache() {
        List<ReviewDto> cached = List.of(testReviewDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<ReviewDto> result = reviewService.getAllReviews();

        assertEquals(cached, result);
        verify(reviewRepository, never()).findAll();
    }

    @Test
    @DisplayName("getAllReviews — кэш пуст, запрос к БД")
    void getAllReviews_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findAll()).thenReturn(List.of(testReview));
        when(reviewMapper.toDto(testReview)).thenReturn(testReviewDto);

        List<ReviewDto> result = reviewService.getAllReviews();

        assertEquals(1, result.size());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getAllReviews — пустой список")
    void getAllReviews_empty() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(reviewService.getAllReviews().isEmpty());
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — из кэша")
    void getById_fromCache() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(testReviewDto);

        ReviewDto result = reviewService.getById(1L);

        assertEquals(testReviewDto, result);
        verify(reviewRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getById — кэш пуст, успех")
    void getById_cacheMiss_success() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewMapper.toDto(testReview)).thenReturn(testReviewDto);

        ReviewDto result = reviewService.getById(1L);

        assertNotNull(result);
        assertEquals(9, result.rating());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getById — отзыв не найден")
    void getById_notFound() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.getById(99L));
    }

    // ─── getReviewsByComicId ──────────────────────────────────────────────────

    @Test
    @DisplayName("getReviewsByComicId — из кэша")
    void getReviewsByComicId_fromCache() {
        List<ReviewDto> cached = List.of(testReviewDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<ReviewDto> result = reviewService.getReviewsByComicId(1L);

        assertEquals(cached, result);
        verify(reviewRepository, never()).findByComicId(any());
    }

    @Test
    @DisplayName("getReviewsByComicId — кэш пуст, запрос к БД")
    void getReviewsByComicId_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findByComicId(1L)).thenReturn(List.of(testReview));
        when(reviewMapper.toDto(testReview)).thenReturn(testReviewDto);

        List<ReviewDto> result = reviewService.getReviewsByComicId(1L);

        assertEquals(1, result.size());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    // ─── addReviewToComic ─────────────────────────────────────────────────────

    @Test
    @DisplayName("addReviewToComic — успех")
    void addReviewToComic_success() {
        // rating в диапазоне 1-10 согласно @Min/@Max
        ReviewRequest request = new ReviewRequest("Шедевр!", 9, 1L);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewMapper.toDto(testReview)).thenReturn(testReviewDto);

        ReviewDto result = reviewService.addReviewToComic(request);

        assertNotNull(result);
        assertEquals(9, result.rating());
        verify(reviewRepository).save(any(Review.class));
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("addReviewToComic — комикс не найден")
    void addReviewToComic_comicNotFound() {
        ReviewRequest request = new ReviewRequest("Текст", 5, 99L);
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.addReviewToComic(request));
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — успех")
    void update_success() {
        ReviewRequest request = new ReviewRequest("Обновлённый текст", 7, 1L);
        ReviewDto updatedDto = new ReviewDto(1L, "Обновлённый текст", 7, 1L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewMapper.toDto(testReview)).thenReturn(updatedDto);

        ReviewDto result = reviewService.update(1L, request);

        assertEquals("Обновлённый текст", result.text());
        assertEquals(7, result.rating());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("update — отзыв не найден")
    void update_notFound() {
        ReviewRequest request = new ReviewRequest("Текст", 5, 1L);
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.update(99L, request));
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — успех")
    void delete_success() {
        when(reviewRepository.existsById(1L)).thenReturn(true);

        reviewService.delete(1L);

        verify(reviewRepository).deleteById(1L);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — отзыв не найден")
    void delete_notFound() {
        when(reviewRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.delete(99L));
    }

    // ─── patch ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("patch — обновляет text и rating")
    void patch_textAndRating() {
        ReviewPatchRequest request = new ReviewPatchRequest("Новый текст", 3, null);
        ReviewDto patchedDto = new ReviewDto(1L, "Новый текст", 3, 1L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewMapper.toDto(testReview)).thenReturn(patchedDto);

        ReviewDto result = reviewService.patch(1L, request);

        assertEquals("Новый текст", result.text());
        assertEquals(3, result.rating());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("patch — обновляет comicId")
    void patch_comicId() {
        Comic newComic = new Comic();
        newComic.setId(2L);
        ReviewPatchRequest request = new ReviewPatchRequest(null, null, 2L);
        ReviewDto patchedDto = new ReviewDto(1L, "Отличный комикс!", 9, 2L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(comicRepository.findById(2L)).thenReturn(Optional.of(newComic));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewMapper.toDto(testReview)).thenReturn(patchedDto);

        ReviewDto result = reviewService.patch(1L, request);

        assertEquals(2L, result.comicId());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("patch — comicId указан, комикс не найден")
    void patch_comicNotFound() {
        ReviewPatchRequest request = new ReviewPatchRequest(null, null, 99L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.patch(1L, request));
    }

    @Test
    @DisplayName("patch — отзыв не найден")
    void patch_reviewNotFound() {
        ReviewPatchRequest request = new ReviewPatchRequest("Текст", 5, null);
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.patch(99L, request));
    }
}