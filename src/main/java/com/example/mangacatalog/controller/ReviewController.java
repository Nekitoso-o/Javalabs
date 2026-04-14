package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.ReviewDto;
import com.example.mangacatalog.dto.ReviewPatchRequest;
import com.example.mangacatalog.dto.ReviewRequest;
import com.example.mangacatalog.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Отзывы", description = "API для управления отзывами пользователей")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Получить все отзывы")
    public List<ReviewDto> getAllReviews() {
        return service.getAllReviews();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить отзыв по ID")
    public ReviewDto getById(@PathVariable("id") Long id) {
        return service.getById(id);
    }

    @GetMapping("/comic/{comicId}")
    @Operation(summary = "Получить все отзывы для конкретного комикса")
    public List<ReviewDto> getReviewsByComicId(@PathVariable("comicId") Long comicId) {
        return service.getReviewsByComicId(comicId);
    }

    @PostMapping
    @Operation(summary = "Оставить отзыв на комикс")
    public ReviewDto createReview(@Valid @RequestBody ReviewRequest request) {
        return service.addReviewToComic(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Отредактировать существующий отзыв")
    public ReviewDto update(@PathVariable("id") Long id, @Valid @RequestBody ReviewRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить отзыв")
    public String delete(@PathVariable("id") Long id) {
        service.delete(id);
        return "Отзыв успешно удален";
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Частично обновить существующий отзыв")
    public ReviewDto patch(@PathVariable("id") Long id, @Valid @RequestBody ReviewPatchRequest request) {
        return service.patch(id, request);
    }
}