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
import org.mockito.Spy;
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

    @Spy
    private ReviewMapper reviewMapper = new ReviewMapper();

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
        testComic.setTitle("Берсерк");

        testReview = new Review();
        testReview.setId(1L);
        testReview.setText("Шедевр!");
        testReview.setRating(10);
        testReview.setComic(testComic);

        testReviewDto = new ReviewDto(1L, "Шедевр!", 10, 1L);
    }



    @Test
    @DisplayName("getAllReviews — кеш попадание")
    void getAllReviews_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class)))
            .thenReturn(List.of(testReviewDto));

        List<ReviewDto> result = reviewService.getAllReviews();

        assertEquals(1, result.size());
        verify(reviewRepository, never()).findAll();
    }

    @Test
    @DisplayName("getAllReviews — кеш промах")
    void getAllReviews_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findAll()).thenReturn(List.of(testReview));

        List<ReviewDto> result = reviewService.getAllReviews();

        assertEquals(1, result.size());
        assertEquals("Шедевр!", result.get(0).text());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getAllReviews — пустой список")
    void getAllReviews_empty() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(reviewService.getAllReviews().isEmpty());
    }



    @Test
    @DisplayName("getById — кеш попадание")
    void getById_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(testReviewDto);

        ReviewDto result = reviewService.getById(1L);

        assertNotNull(result);
        verify(reviewRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getById — кеш промах, успех")
    void getById_cacheMiss_success() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        ReviewDto result = reviewService.getById(1L);

        assertNotNull(result);
        assertEquals("Шедевр!", result.text());
        assertEquals(10, result.rating());
    }

    @Test
    @DisplayName("getById — не найден")
    void getById_notFound() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.getById(99L));
    }


    @Test
    @DisplayName("getReviewsByComicId — кеш попадание")
    void getReviewsByComicId_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class)))
            .thenReturn(List.of(testReviewDto));

        List<ReviewDto> result = reviewService.getReviewsByComicId(1L);

        assertEquals(1, result.size());
        verify(reviewRepository, never()).findByComicId(any());
    }

    @Test
    @DisplayName("getReviewsByComicId — кеш промах")
    void getReviewsByComicId_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findByComicId(1L)).thenReturn(List.of(testReview));

        List<ReviewDto> result = reviewService.getReviewsByComicId(1L);

        assertEquals(1, result.size());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getReviewsByComicId — пустой список")
    void getReviewsByComicId_empty() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findByComicId(1L)).thenReturn(Collections.emptyList());

        assertTrue(reviewService.getReviewsByComicId(1L).isEmpty());
    }


    @Test
    @DisplayName("addReviewToComic — успех")
    void addReviewToComic_success() {
        ReviewRequest request = new ReviewRequest("Шедевр!", 10, 1L);
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        ReviewDto result = reviewService.addReviewToComic(request);

        assertNotNull(result);
        assertEquals("Шедевр!", result.text());
        assertEquals(10, result.rating());
        assertEquals(1L, result.comicId());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("addReviewToComic — комикс не найден")
    void addReviewToComic_comicNotFound() {
        ReviewRequest request = new ReviewRequest("Текст", 5, 99L);
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.addReviewToComic(request));
        verify(reviewRepository, never()).save(any());
        verify(cacheManager, never()).invalidate();
    }


    @Test
    @DisplayName("update — успешное обновление")
    void update_success() {
        ReviewRequest request = new ReviewRequest("Обновлённый текст", 8, 1L);
        Review updated = new Review();
        updated.setId(1L);
        updated.setText("Обновлённый текст");
        updated.setRating(8);
        updated.setComic(testComic);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(updated);

        ReviewDto result = reviewService.update(1L, request);

        assertEquals("Обновлённый текст", result.text());
        assertEquals(8, result.rating());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("update — не найден")
    void update_notFound() {
        ReviewRequest request = new ReviewRequest("Текст", 5, 1L);
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.update(99L, request));
        verify(cacheManager, never()).invalidate();
    }

    @Test
    @DisplayName("delete — успех")
    void delete_success() {
        when(reviewRepository.existsById(1L)).thenReturn(true);
        doNothing().when(reviewRepository).deleteById(1L);

        reviewService.delete(1L);

        verify(reviewRepository).deleteById(1L);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — не найден")
    void delete_notFound() {
        when(reviewRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.delete(99L));
        verify(reviewRepository, never()).deleteById(any());
        verify(cacheManager, never()).invalidate();
    }

    @Test
    @DisplayName("patch — обновление всех полей")
    void patch_allFields() {
        ReviewPatchRequest request = new ReviewPatchRequest("Новый текст", 7, 1L);
        Comic newComic = new Comic();
        newComic.setId(1L);
        Review saved = new Review();
        saved.setId(1L);
        saved.setText("Новый текст");
        saved.setRating(7);
        saved.setComic(newComic);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(comicRepository.findById(1L)).thenReturn(Optional.of(newComic));
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ReviewDto result = reviewService.patch(1L, request);

        assertNotNull(result);
        assertEquals("Новый текст", result.text());
        assertEquals(7, result.rating());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("patch — все поля null, изменений нет")
    void patch_noFieldsChanged() {
        ReviewPatchRequest request = new ReviewPatchRequest(null, null, null);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        ReviewDto result = reviewService.patch(1L, request);

        assertNotNull(result);
        verify(comicRepository, never()).findById(any());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("patch — только текст обновляется")
    void patch_onlyText() {
        ReviewPatchRequest request = new ReviewPatchRequest("Только текст", null, null);
        Review saved = new Review();
        saved.setId(1L);
        saved.setText("Только текст");
        saved.setRating(10);
        saved.setComic(testComic);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ReviewDto result = reviewService.patch(1L, request);

        assertEquals("Только текст", result.text());
        verify(comicRepository, never()).findById(any());
    }

    @Test
    @DisplayName("patch — только рейтинг обновляется")
    void patch_onlyRating() {
        ReviewPatchRequest request = new ReviewPatchRequest(null, 5, null);
        Review saved = new Review();
        saved.setId(1L);
        saved.setText("Шедевр!");
        saved.setRating(5);
        saved.setComic(testComic);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ReviewDto result = reviewService.patch(1L, request);

        assertEquals(5, result.rating());
        verify(comicRepository, never()).findById(any());
    }

    @Test
    @DisplayName("patch — comicId не найден")
    void patch_comicNotFound() {
        ReviewPatchRequest request = new ReviewPatchRequest(null, null, 99L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.patch(1L, request));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("patch — отзыв не найден")
    void patch_reviewNotFound() {
        ReviewPatchRequest request = new ReviewPatchRequest("Текст", 5, null);
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.patch(99L, request));
        verify(reviewRepository, never()).save(any());
    }
}