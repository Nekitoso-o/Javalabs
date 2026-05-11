package com.example.mangacatalog.repository;

import com.example.mangacatalog.entity.ComicChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComicChapterRepository extends JpaRepository<ComicChapter, Long> {

    @Query("SELECT c FROM ComicChapter c WHERE c.comic.id = :comicId " +
        "ORDER BY c.chapterNumber ASC")
    List<ComicChapter> findByComicIdOrdered(@Param("comicId") Long comicId);

    boolean existsByComicIdAndChapterNumber(Long comicId, Double chapterNumber);

    Optional<ComicChapter> findByComicIdAndChapterNumber(Long comicId, Double chapterNumber);
}