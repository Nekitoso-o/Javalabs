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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private Review review;
    private ReviewDto reviewDto;
    private Comic comic;

    @BeforeEach
    void setUp() {
        comic = new Comic();
        comic.setId(10L);
        comic.setTitle("Test Comic");

        review = new Review();
        review.setId(1L);
        review.setText("Great!");
        review.setRating(9);
        review.setComic(comic);

        reviewDto = new ReviewDto(1L, "Great!", 9, 10L);
    }

    // getAllReviews()

    @Test
    void getAllReviews_whenCacheHit_returnsCached() {
        List<ReviewDto> cached = List.of(reviewDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<ReviewDto> result = reviewService.getAllReviews();

        assertThat(result).isEqualTo(cached);
        verify(reviewRepository, never()).findAll();
    }

    @Test
    void getAllReviews_whenCacheMiss_fetchesAndCaches() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findAll()).thenReturn(List.of(review));
        when(reviewMapper.toDto(review)).thenReturn(reviewDto);

        List<ReviewDto> result = reviewService.getAllReviews();

        assertThat(result).containsExactly(reviewDto);
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    // getById()

    @Test
    void getById_whenCacheHit_returnsCached() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(reviewDto);

        ReviewDto result = reviewService.getById(1L);

        assertThat(result).isEqualTo(reviewDto);
        verify(reviewRepository, never()).findById(any());
    }

    @Test
    void getById_whenCacheMiss_fetchesFromDb() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewMapper.toDto(review)).thenReturn(reviewDto);

        ReviewDto result = reviewService.getById(1L);

        assertThat(result).isEqualTo(reviewDto);
        verify(cacheManager).put(any(ApiCacheKey.class), eq(reviewDto));
    }

    @Test
    void getById_whenNotFound_throwsException() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // getReviewsByComicId()

    @Test
    void getReviewsByComicId_whenCacheHit_returnsCached() {
        List<ReviewDto> cached = List.of(reviewDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<ReviewDto> result = reviewService.getReviewsByComicId(10L);

        assertThat(result).isEqualTo(cached);
    }

    @Test
    void getReviewsByComicId_whenCacheMiss_fetchesAndCaches() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(reviewRepository.findByComicId(10L)).thenReturn(List.of(review));
        when(reviewMapper.toDto(review)).thenReturn(reviewDto);

        List<ReviewDto> result = reviewService.getReviewsByComicId(10L);

        assertThat(result).containsExactly(reviewDto);
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    // addReviewToComic()

    @Test
    void addReviewToComic_whenComicExists_createsReview() {
        ReviewRequest request = new ReviewRequest("Nice", 8, 10L);
        Review savedReview = new Review();
        savedReview.setId(2L);
        savedReview.setText("Nice");
        savedReview.setRating(8);
        savedReview.setComic(comic);
        ReviewDto savedDto = new ReviewDto(2L, "Nice", 8, 10L);

        when(comicRepository.findById(10L)).thenReturn(Optional.of(comic));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(reviewMapper.toDto(savedReview)).thenReturn(savedDto);

        ReviewDto result = reviewService.addReviewToComic(request);

        assertThat(result).isEqualTo(savedDto);
        verify(cacheManager).invalidate();
    }

    @Test
    void addReviewToComic_whenComicNotFound_throwsException() {
        ReviewRequest request = new ReviewRequest("Text", 5, 99L);
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.addReviewToComic(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // update()

    @Test
    void update_whenReviewExists_updatesAndInvalidates() {
        ReviewRequest request = new ReviewRequest("Updated", 7, 10L);
        Review updated = new Review();
        updated.setId(1L);
        updated.setText("Updated");
        updated.setRating(7);
        ReviewDto updatedDto = new ReviewDto(1L, "Updated", 7, 10L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(updated);
        when(reviewMapper.toDto(updated)).thenReturn(updatedDto);

        ReviewDto result = reviewService.update(1L, request);

        assertThat(result).isEqualTo(updatedDto);
        verify(cacheManager).invalidate();
    }

    @Test
    void update_whenNotFound_throwsException() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.update(99L, new ReviewRequest("t", 5, 1L)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // delete()

    @Test
    void delete_whenExists_deletesAndInvalidates() {
        when(reviewRepository.existsById(1L)).thenReturn(true);

        reviewService.delete(1L);

        verify(reviewRepository).deleteById(1L);
        verify(cacheManager).invalidate();
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(reviewRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // patch()

    @Test
    void patch_whenAllFieldsProvided_updatesAllFields() {
        ReviewPatchRequest request = new ReviewPatchRequest("Patched text", 6, 10L);
        Comic newComic = new Comic();
        newComic.setId(10L);

        Review saved = new Review();
        saved.setId(1L);
        saved.setText("Patched text");
        saved.setRating(6);
        saved.setComic(newComic);
        ReviewDto savedDto = new ReviewDto(1L, "Patched text", 6, 10L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(comicRepository.findById(10L)).thenReturn(Optional.of(newComic));
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);
        when(reviewMapper.toDto(saved)).thenReturn(savedDto);

        ReviewDto result = reviewService.patch(1L, request);

        assertThat(result).isEqualTo(savedDto);
        verify(cacheManager).invalidate();
    }

    @Test
    void patch_whenOnlyTextProvided_updatesOnlyText() {
        ReviewPatchRequest request = new ReviewPatchRequest("Only text", null, null);
        Review saved = new Review();
        saved.setId(1L);
        saved.setText("Only text");
        saved.setRating(9);
        ReviewDto savedDto = new ReviewDto(1L, "Only text", 9, 10L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);
        when(reviewMapper.toDto(saved)).thenReturn(savedDto);

        ReviewDto result = reviewService.patch(1L, request);

        assertThat(result).isEqualTo(savedDto);
        verify(comicRepository, never()).findById(any());
    }

    @Test
    void patch_whenOnlyRatingProvided_updatesOnlyRating() {
        ReviewPatchRequest request = new ReviewPatchRequest(null, 3, null);
        Review saved = new Review();
        saved.setId(1L);
        saved.setText("Great!");
        saved.setRating(3);
        ReviewDto savedDto = new ReviewDto(1L, "Great!", 3, 10L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);
        when(reviewMapper.toDto(saved)).thenReturn(savedDto);

        ReviewDto result = reviewService.patch(1L, request);

        assertThat(result).isEqualTo(savedDto);
    }

    @Test
    void patch_whenComicIdProvided_butComicNotFound_throwsException() {
        ReviewPatchRequest request = new ReviewPatchRequest(null, null, 999L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(comicRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.patch(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void patch_whenReviewNotFound_throwsException() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.patch(99L, new ReviewPatchRequest(null, null, null)))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}