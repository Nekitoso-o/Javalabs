package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.ReviewDto;
import com.example.mangacatalog.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService service;

    @GetMapping
    public List<ReviewDto> getAllReviews() {
        return service.getAllReviews();
    }

    @GetMapping("/{id}")
    public ReviewDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/comic/{comicId}")
    public List<ReviewDto> getReviewsByComicId(@PathVariable Long comicId) {
        return service.getReviewsByComicId(comicId);
    }

    @PostMapping
    public ReviewDto createReview(@RequestBody ReviewDto dto) {
        return service.addReviewToComic(dto);
    }

    @PutMapping("/{id}")
    public ReviewDto update(@PathVariable Long id, @RequestBody ReviewDto dto) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}")
    public ReviewDto patch(@PathVariable Long id, @RequestBody ReviewDto dto) {
        return service.patch(id, dto);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Review deleted successfully";
    }
}