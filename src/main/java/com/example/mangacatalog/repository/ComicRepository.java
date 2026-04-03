package com.example.mangacatalog.repository;

import com.example.mangacatalog.entity.Comic;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComicRepository extends JpaRepository<Comic, Long> {

    @EntityGraph(attributePaths = {"author", "publisher", "genres"})
    @Query("SELECT c FROM Comic c")
    List<Comic> findAllWithoutNPlusOne();

    List<Comic> findByTitleContainingIgnoreCase(String title);
}