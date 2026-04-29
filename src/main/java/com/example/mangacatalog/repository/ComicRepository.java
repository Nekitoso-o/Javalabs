package com.example.mangacatalog.repository;

import com.example.mangacatalog.dto.ComicNativeProjection;
import com.example.mangacatalog.entity.Comic;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComicRepository extends JpaRepository<Comic, Long> {


    @Override
    @EntityGraph(attributePaths = {"author", "publisher", "genres"})
    List<Comic> findAll();

    @Override
    @EntityGraph(attributePaths = {"author", "publisher", "genres"})
    Optional<Comic> findById(Long id);

    @EntityGraph(attributePaths = {"author", "publisher", "genres"})
    List<Comic> findByTitleContainingIgnoreCase(String title);

    @EntityGraph(attributePaths = {"author", "publisher", "genres"})
    List<Comic> findByAuthorId(Long authorId);


    @Query("SELECT DISTINCT c FROM Comic c "
        + "LEFT JOIN FETCH c.author "
        + "LEFT JOIN FETCH c.publisher "
        + "LEFT JOIN FETCH c.genres "
        + "WHERE c.id IN ("
        + "  SELECT DISTINCT c2.id FROM Comic c2 "
        + "  LEFT JOIN c2.genres g "
        + "  WHERE (:genreName IS NULL OR g.name = :genreName) "
        + "  AND (:minYear IS NULL OR c2.releaseYear >= :minYear)"
        + ") "
        + "ORDER BY c.id")
    List<Comic> findByGenreAndYearJpql(
        @Param("genreName") String genreName,
        @Param("minYear") Integer minYear,
        Pageable pageable
    );

    @Query(value = "SELECT c.id AS id, c.title AS title, "
        + "c.release_year AS releaseYear, "
        + "a.id AS authorId, a.name AS authorName, "
        + "p.id AS publisherId, p.name AS publisherName, "
        + "STRING_AGG(DISTINCT CAST(g.id AS VARCHAR), ',') AS genreIds, "
        + "STRING_AGG(DISTINCT g.name, ',') AS genreNames "
        + "FROM comics c "
        + "LEFT JOIN authors a ON c.author_id = a.id "
        + "LEFT JOIN publishers p ON c.publisher_id = p.id "
        + "LEFT JOIN comic_genres cg ON c.id = cg.comic_id "
        + "LEFT JOIN genres g ON cg.genre_id = g.id "
        + "WHERE c.id IN ("
        + "  SELECT c_inner.id FROM comics c_inner "
        + "  JOIN comic_genres cg_inner ON c_inner.id = cg_inner.comic_id "
        + "  JOIN genres g_inner ON cg_inner.genre_id = g_inner.id "
        + "  WHERE (:genreName IS NULL OR g_inner.name = :genreName) "
        + "  AND (:minYear IS NULL OR c_inner.release_year >= :minYear)"
        + ") "
        + "GROUP BY c.id, c.title, c.release_year, a.id, a.name, p.id, p.name "
        + "ORDER BY c.id",
        nativeQuery = true)
    List<ComicNativeProjection> findByGenreAndYearNative(
        @Param("genreName") String genreName,
        @Param("minYear") Integer minYear,
        Pageable pageable
    );

}