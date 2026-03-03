package com.example.mangacatalog.repository;

import com.example.mangacatalog.entity.Comic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComicRepository extends JpaRepository<Comic, Long> {
    // Метод для поиска по названию (для реализации @RequestParam)
    List<Comic> findByTitleContainingIgnoreCase(String title);
}