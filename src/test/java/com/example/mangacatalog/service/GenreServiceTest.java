package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.GenreDto;
import com.example.mangacatalog.dto.GenreRequest;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Genre;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.mapper.GenreMapper;
import com.example.mangacatalog.repository.GenreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    private GenreRepository repository;
    @Mock
    private GenreMapper mapper;
    @Mock
    private ApiCacheManager cacheManager;

    @InjectMocks
    private GenreService genreService;

    private Genre testGenre;
    private GenreDto testGenreDto;

    @BeforeEach
    void setUp() {
        testGenre = new Genre();
        testGenre.setId(1L);
        testGenre.setName("Сёнэн");
        testGenreDto = new GenreDto(1L, "Сёнэн");
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll — из кэша")
    void getAll_fromCache() {
        List<GenreDto> cached = List.of(testGenreDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<GenreDto> result = genreService.getAll();

        assertEquals(cached, result);
        verify(repository, never()).findAll();
    }

    @Test
    @DisplayName("getAll — кэш пуст, запрос к БД")
    void getAll_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(List.of(testGenre));
        when(mapper.toDto(testGenre)).thenReturn(testGenreDto);

        List<GenreDto> result = genreService.getAll();

        assertEquals(1, result.size());
        assertEquals("Сёнэн", result.get(0).name());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getAll — пустой список")
    void getAll_empty() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(genreService.getAll().isEmpty());
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — из кэша")
    void getById_fromCache() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(testGenreDto);

        GenreDto result = genreService.getById(1L);

        assertEquals(testGenreDto, result);
        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("getById — кэш пуст, успех")
    void getById_cacheMiss_success() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));
        when(mapper.toDto(testGenre)).thenReturn(testGenreDto);

        GenreDto result = genreService.getById(1L);

        assertNotNull(result);
        assertEquals("Сёнэн", result.name());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getById — жанр не найден")
    void getById_notFound() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> genreService.getById(99L));
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — успех")
    void create_success() {
        GenreRequest request = new GenreRequest("Сэйнэн");
        Genre saved = new Genre();
        saved.setId(2L);
        saved.setName("Сэйнэн");
        GenreDto savedDto = new GenreDto(2L, "Сэйнэн");

        when(repository.save(any(Genre.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(savedDto);

        GenreDto result = genreService.create(request);

        assertNotNull(result);
        assertEquals("Сэйнэн", result.name());
        verify(cacheManager).invalidate();
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — успех")
    void update_success() {
        GenreRequest request = new GenreRequest("Сёдзё");
        GenreDto updatedDto = new GenreDto(1L, "Сёдзё");

        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));
        when(repository.save(any(Genre.class))).thenReturn(testGenre);
        when(mapper.toDto(testGenre)).thenReturn(updatedDto);

        GenreDto result = genreService.update(1L, request);

        assertEquals("Сёдзё", result.name());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("update — жанр не найден")
    void update_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> genreService.update(99L, new GenreRequest("Имя")));
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — успех, комикс отвязывается от жанра")
    void delete_success_withComics() {
        // Genre.comics — без сеттера, заполняем через getComics().add()
        Comic comic = new Comic();
        // genres у Comic — HashSet, инициализирован в сущности
        comic.getGenres().add(testGenre);
        testGenre.getComics().add(comic);

        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));

        genreService.delete(1L);

        assertFalse(comic.getGenres().contains(testGenre));
        verify(repository).delete(testGenre);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — успех, у жанра нет комиксов")
    void delete_success_noComics() {
        // getComics() вернёт пустой список (инициализирован в Genre)
        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));

        genreService.delete(1L);

        verify(repository).delete(testGenre);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — жанр не найден")
    void delete_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> genreService.delete(99L));
    }
}