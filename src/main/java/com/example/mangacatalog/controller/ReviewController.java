package com.example.mangacatalog.controller;
import com.example.mangacatalog.dto.ReviewDto;
import com.example.mangacatalog.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    @PostMapping
    public ReviewDto createReview(@RequestBody ReviewDto dto) {
        return service.addReviewToComic(dto);
    }
}