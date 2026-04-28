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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    private GenreRepository repository;

    @Spy
    private GenreMapper mapper = new GenreMapper();

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


    @Test
    @DisplayName("getAll — кеш попадание")
    void getAll_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class)))
            .thenReturn(List.of(testGenreDto));

        List<GenreDto> result = genreService.getAll();

        assertEquals(1, result.size());
        verify(repository, never()).findAll();
    }

    @Test
    @DisplayName("getAll — кеш промах")
    void getAll_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(List.of(testGenre));

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



    @Test
    @DisplayName("getById — кеш попадание")
    void getById_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(testGenreDto);

        GenreDto result = genreService.getById(1L);

        assertNotNull(result);
        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("getById — кеш промах, успех")
    void getById_cacheMiss_success() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));

        GenreDto result = genreService.getById(1L);

        assertNotNull(result);
        assertEquals("Сёнэн", result.name());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getById — не найден")
    void getById_notFound() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> genreService.getById(99L));
    }



    @Test
    @DisplayName("create — успешное создание")
    void create_success() {
        GenreRequest request = new GenreRequest("Сёнэн");
        when(repository.save(any(Genre.class))).thenReturn(testGenre);

        GenreDto result = genreService.create(request);

        assertNotNull(result);
        assertEquals("Сёнэн", result.name());
        verify(cacheManager).invalidate();
    }



    @Test
    @DisplayName("update — успешное обновление")
    void update_success() {
        GenreRequest request = new GenreRequest("Сэйнэн");
        Genre updated = new Genre();
        updated.setId(1L);
        updated.setName("Сэйнэн");

        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));
        when(repository.save(any(Genre.class))).thenReturn(updated);

        GenreDto result = genreService.update(1L, request);

        assertEquals("Сэйнэн", result.name());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("update — не найден")
    void update_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> genreService.update(99L, new GenreRequest("X")));
        verify(repository, never()).save(any());
    }


    @Test
    @DisplayName("delete — успешное удаление без комиксов")
    void delete_success_noComics() {
        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));

        genreService.delete(1L);

        verify(repository).delete(testGenre);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — жанр удаляется из связанных комиксов")
    void delete_removesGenreFromComics() {
        Comic comic = new Comic();
        comic.setId(5L);
        comic.getGenres().add(testGenre);
        testGenre.getComics().add(comic);

        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));

        genreService.delete(1L);

        assertFalse(comic.getGenres().contains(testGenre));
        verify(repository).delete(testGenre);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — не найден")
    void delete_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> genreService.delete(99L));
        verify(repository, never()).delete(any(Genre.class));
    }
}