package com.example.mangacatalog.repository;

import com.example.mangacatalog.entity.ComicImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComicImageRepository extends JpaRepository<ComicImage, Long> {
    List<ComicImage> findByComicIdOrderBySortOrderAsc(Long comicId);
    void deleteByComicId(Long comicId);
}