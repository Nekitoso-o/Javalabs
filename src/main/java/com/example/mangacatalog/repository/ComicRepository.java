package com.example.mangacatalog.repository;

import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.dto.ComicNativeProjection;
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


    @EntityGraph(attributePaths = {"author", "publisher", "genres"})
    @Query("SELECT c FROM Comic c")
    List<Comic> findAllWithoutNPlusOne();

    @EntityGraph(attributePaths = {"author", "publisher", "genres"})
    @Query("SELECT c FROM Comic c JOIN c.genres g WHERE g.name = :genreName AND c.releaseYear >= :minYear")
    List<Comic> findByGenreAndYearJpql(
        @Param("genreName") String genreName,
        @Param("minYear") Integer minYear,
        Pageable pageable
    );

    @Query(value = "SELECT c.id as id, c.title as title, c.release_year as releaseYear, " +
        "a.id as authorId, a.name as authorName, " +
        "p.id as publisherId, p.name as publisherName, " +
        "STRING_AGG(DISTINCT CAST(g.id AS VARCHAR), ',') as genreIds, " +
        "STRING_AGG(DISTINCT g.name, ',') as genreNames " +
        "FROM comics c " +
        "LEFT JOIN authors a ON c.author_id = a.id " +
        "LEFT JOIN publishers p ON c.publisher_id = p.id " +
        "LEFT JOIN comic_genres cg ON c.id = cg.comic_id " +
        "LEFT JOIN genres g ON cg.genre_id = g.id " +
        "WHERE c.id IN (" +
        "   SELECT c_inner.id FROM comics c_inner " +
        "   JOIN comic_genres cg_inner ON c_inner.id = cg_inner.comic_id " +
        "   JOIN genres g_inner ON cg_inner.genre_id = g_inner.id " +
        "   WHERE g_inner.name = :genreName AND c_inner.release_year >= :minYear" +
        ") " +
        "GROUP BY c.id, c.title, c.release_year, a.id, a.name, p.id, p.name",
        nativeQuery = true)
    List<ComicNativeProjection> findByGenreAndYearNative(
        @Param("genreName") String genreName,
        @Param("minYear") Integer minYear,
        Pageable pageable
    );
}