package com.example.mangacatalog.repository;

import com.example.mangacatalog.entity.ChapterPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterPageRepository extends JpaRepository<ChapterPage, Long> {
    List<ChapterPage> findByChapterIdOrderBySortOrderAsc(Long chapterId);
}