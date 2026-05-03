package com.example.mangacatalog.service;

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

    private final ReviewMapper reviewMapper = new ReviewMapper();
    private final ApiCacheManager cacheManager = new ApiCacheManager();

    private ReviewService reviewService;
    private Comic testComic;
    private Review testReview;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
            reviewRepository, comicRepository, reviewMapper, cacheManager);
        cacheManager.invalidate();

        testComic = new Comic();
        testComic.setId(1L);
        testComic.setTitle("One Piece");

        testReview = new Review();
        testReview.setId(1L);
        testReview.setText("Отличный комикс!");
        testReview.setRating(9);
        testReview.setComic(testComic);
    }

    // ─── getAllReviews ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllReviews — кэш пуст, запрос к БД")
    void getAllReviews_cacheMiss() {
        when(reviewRepository.findAll()).thenReturn(List.of(testReview));

        List<ReviewDto> result = reviewService.getAllReviews();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Отличный комикс!", result.get(0).text());
        assertEquals(9, result.get(0).rating());
        assertEquals(1L, result.get(0).comicId());
        verify(reviewRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllReviews — второй вызов из кэша")
    void getAllReviews_secondCall_fromCache() {
        when(reviewRepository.findAll()).thenReturn(List.of(testReview));

        reviewService.getAllReviews();
        reviewService.getAllReviews();

        verify(reviewRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllReviews — пустой список")
    void getAllReviews_empty() {
        when(reviewRepository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(reviewService.getAllReviews().isEmpty());
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — успех")
    void getById_success() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        ReviewDto result = reviewService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(9, result.rating());
        assertEquals(1L, result.comicId());
    }

    @Test
    @DisplayName("getById — второй вызов из кэша")
    void getById_secondCall_fromCache() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        reviewService.getById(1L);
        reviewService.getById(1L);

        verify(reviewRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getById — не найден")
    void getById_notFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.getById(99L));
    }

    // ─── getReviewsByComicId ──────────────────────────────────────────────────

    @Test
    @DisplayName("getReviewsByComicId — успех")
    void getReviewsByComicId_success() {
        when(reviewRepository.findByComicId(1L)).thenReturn(List.of(testReview));

        List<ReviewDto> result = reviewService.getReviewsByComicId(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).comicId());
    }

    @Test
    @DisplayName("getReviewsByComicId — второй вызов из кэша")
    void getReviewsByComicId_secondCall_fromCache() {
        when(reviewRepository.findByComicId(1L)).thenReturn(List.of(testReview));

        reviewService.getReviewsByComicId(1L);
        reviewService.getReviewsByComicId(1L);

        verify(reviewRepository, times(1)).findByComicId(1L);
    }

    // ─── addReviewToComic ─────────────────────────────────────────────────────

    @Test
    @DisplayName("addReviewToComic — успех")
    void addReviewToComic_success() {
        ReviewRequest request = new ReviewRequest("Шедевр!", 9, 1L);
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        ReviewDto result = reviewService.addReviewToComic(request);

        assertNotNull(result);
        assertEquals(9, result.rating());
        assertEquals(1L, result.comicId());
        verify(reviewRepository).save(any(Review.class));
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
        Review updated = new Review();
        updated.setId(1L);
        updated.setText("Обновлённый текст");
        updated.setRating(7);
        updated.setComic(testComic);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(updated);

        ReviewDto result = reviewService.update(1L, request);

        assertEquals("Обновлённый текст", result.text());
        assertEquals(7, result.rating());
    }

    @Test
    @DisplayName("update — не найден")
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
    }

    @Test
    @DisplayName("delete — не найден")
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
        Review patched = new Review();
        patched.setId(1L);
        patched.setText("Новый текст");
        patched.setRating(3);
        patched.setComic(testComic);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(patched);

        ReviewDto result = reviewService.patch(1L, request);

        assertEquals("Новый текст", result.text());
        assertEquals(3, result.rating());
    }

    @Test
    @DisplayName("patch — обновляет comicId")
    void patch_comicId() {
        Comic newComic = new Comic();
        newComic.setId(2L);
        ReviewPatchRequest request = new ReviewPatchRequest(null, null, 2L);
        Review patched = new Review();
        patched.setId(1L);
        patched.setText("Отличный комикс!");
        patched.setRating(9);
        patched.setComic(newComic);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(comicRepository.findById(2L)).thenReturn(Optional.of(newComic));
        when(reviewRepository.save(any(Review.class))).thenReturn(patched);

        ReviewDto result = reviewService.patch(1L, request);

        assertEquals(2L, result.comicId());
    }

    @Test
    @DisplayName("patch — comicId не найден")
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