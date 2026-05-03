package com.example.mangacatalog.service;

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
import org.mockito.Mock;
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

    private final GenreMapper mapper = new GenreMapper();
    private final ApiCacheManager cacheManager = new ApiCacheManager();

    private GenreService genreService;
    private Genre testGenre;

    @BeforeEach
    void setUp() {
        genreService = new GenreService(repository, mapper, cacheManager);
        cacheManager.invalidate();

        testGenre = new Genre();
        testGenre.setId(1L);
        testGenre.setName("Сёнэн");
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll — кэш пуст, запрос к БД")
    void getAll_cacheMiss() {
        when(repository.findAll()).thenReturn(List.of(testGenre));

        List<GenreDto> result = genreService.getAll();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Сёнэн", result.get(0).name());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll — второй вызов из кэша")
    void getAll_secondCall_fromCache() {
        when(repository.findAll()).thenReturn(List.of(testGenre));

        genreService.getAll();
        genreService.getAll();

        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll — пустой список")
    void getAll_empty() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(genreService.getAll().isEmpty());
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — успех")
    void getById_success() {
        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));

        GenreDto result = genreService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Сёнэн", result.name());
    }

    @Test
    @DisplayName("getById — второй вызов из кэша")
    void getById_secondCall_fromCache() {
        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));

        genreService.getById(1L);
        genreService.getById(1L);

        verify(repository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getById — жанр не найден")
    void getById_notFound() {
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
        when(repository.save(any(Genre.class))).thenReturn(saved);

        GenreDto result = genreService.create(request);

        assertNotNull(result);
        assertEquals(2L, result.id());
        assertEquals("Сэйнэн", result.name());
    }

    @Test
    @DisplayName("create — кэш инвалидируется")
    void create_invalidatesCache() {
        when(repository.findAll()).thenReturn(List.of(testGenre));
        genreService.getAll();

        Genre saved = new Genre();
        saved.setId(2L);
        saved.setName("Сэйнэн");
        when(repository.save(any(Genre.class))).thenReturn(saved);
        genreService.create(new GenreRequest("Сэйнэн"));

        when(repository.findAll()).thenReturn(List.of(testGenre, saved));
        genreService.getAll();
        verify(repository, times(2)).findAll();
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — успех")
    void update_success() {
        GenreRequest request = new GenreRequest("Сёдзё");
        Genre updated = new Genre();
        updated.setId(1L);
        updated.setName("Сёдзё");
        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));
        when(repository.save(any(Genre.class))).thenReturn(updated);

        GenreDto result = genreService.update(1L, request);

        assertEquals("Сёдзё", result.name());
        verify(repository).save(any(Genre.class));
    }

    @Test
    @DisplayName("update — жанр не найден")
    void update_notFound() {
        GenreRequest request = new GenreRequest("Имя");
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> genreService.update(99L, request));
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — успех, комикс отвязывается от жанра")
    void delete_success_withComics() {
        Comic comic = new Comic();
        comic.getGenres().add(testGenre);
        testGenre.getComics().add(comic);
        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));

        genreService.delete(1L);

        assertFalse(comic.getGenres().contains(testGenre));
        verify(repository).delete(testGenre);
    }

    @Test
    @DisplayName("delete — успех, нет комиксов")
    void delete_success_noComics() {
        when(repository.findById(1L)).thenReturn(Optional.of(testGenre));

        genreService.delete(1L);

        verify(repository).delete(testGenre);
    }

    @Test
    @DisplayName("delete — жанр не найден")
    void delete_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> genreService.delete(99L));
    }

    // Строка 88: if (genre.getComics() != null) — ветка false (null)
// Нужен тест где getComics() возвращает null
    @Test
    @DisplayName("delete — getComics() возвращает null, не падает")
    void delete_success_comicsIsNull() {
        // Создаём жанр через мок чтобы getComics() вернул null
        Genre genreWithNullComics = mock(Genre.class);
        when(genreWithNullComics.getComics()).thenReturn(null);
        when(repository.findById(2L)).thenReturn(Optional.of(genreWithNullComics));

        // Не должно выбросить NullPointerException
        genreService.delete(2L);

        verify(repository).delete(genreWithNullComics);
    }

    // Строка 88: покрываем ветку где список пустой (не null)
// и ветку где список null — но Genre инициализирует список в конструкторе,
// поэтому null никогда не будет. Sonar это тоже знает.
// Добавляем тест с пустым списком комиксов явно
    @Test
    @DisplayName("delete — успех, пустой список комиксов")
    void delete_success_emptyComicsList() {
        // Genre инициализирует comics как new ArrayList() — список пустой, не null
        Genre emptyGenre = new Genre();
        emptyGenre.setId(3L);
        emptyGenre.setName("Пустой жанр");
        // getComics() вернёт пустой ArrayList

        when(repository.findById(3L)).thenReturn(Optional.of(emptyGenre));

        genreService.delete(3L);

        verify(repository).delete(emptyGenre);
    }
}