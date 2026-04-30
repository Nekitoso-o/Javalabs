package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.AuthorDto;
import com.example.mangacatalog.dto.AuthorRequest;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.mapper.AuthorMapper;
import com.example.mangacatalog.repository.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository repository;
    @Mock
    private AuthorMapper mapper;
    @Mock
    private ApiCacheManager cacheManager;

    @InjectMocks
    private AuthorService authorService;

    private Author testAuthor;
    private AuthorDto testAuthorDto;

    @BeforeEach
    void setUp() {
        testAuthor = new Author();
        testAuthor.setId(1L);
        testAuthor.setName("Акира Торияма");
        testAuthorDto = new AuthorDto(1L, "Акира Торияма");
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll — возвращает список из кэша")
    void getAll_returnsFromCache() {
        List<AuthorDto> cached = List.of(testAuthorDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<AuthorDto> result = authorService.getAll();

        assertEquals(cached, result);
        verify(repository, never()).findAll();
        verify(cacheManager, never()).put(any(), any());
    }

    @Test
    @DisplayName("getAll — кэш пуст, запрос к БД")
    void getAll_cacheMiss_queriesDb() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(List.of(testAuthor));
        when(mapper.toDto(testAuthor)).thenReturn(testAuthorDto);

        List<AuthorDto> result = authorService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Акира Торияма", result.get(0).name());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getAll — пустой список")
    void getAll_empty() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(authorService.getAll().isEmpty());
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — из кэша")
    void getById_fromCache() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(testAuthorDto);

        AuthorDto result = authorService.getById(1L);

        assertEquals(testAuthorDto, result);
        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("getById — кэш пуст, успех")
    void getById_cacheMiss_success() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(mapper.toDto(testAuthor)).thenReturn(testAuthorDto);

        AuthorDto result = authorService.getById(1L);

        assertNotNull(result);
        assertEquals("Акира Торияма", result.name());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getById — автор не найден")
    void getById_notFound() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> authorService.getById(99L));
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — успех")
    void create_success() {
        AuthorRequest request = new AuthorRequest("Масаси Кисимото");
        Author saved = new Author();
        saved.setId(2L);
        saved.setName("Масаси Кисимото");
        AuthorDto savedDto = new AuthorDto(2L, "Масаси Кисимото");

        when(repository.save(any(Author.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(savedDto);

        AuthorDto result = authorService.create(request);

        assertNotNull(result);
        assertEquals("Масаси Кисимото", result.name());
        verify(repository).save(any(Author.class));
        verify(cacheManager).invalidate();
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — успех")
    void update_success() {
        AuthorRequest request = new AuthorRequest("Новое Имя");
        AuthorDto updatedDto = new AuthorDto(1L, "Новое Имя");

        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(repository.save(any(Author.class))).thenReturn(testAuthor);
        when(mapper.toDto(testAuthor)).thenReturn(updatedDto);

        AuthorDto result = authorService.update(1L, request);

        assertNotNull(result);
        assertEquals("Новое Имя", result.name());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("update — автор не найден")
    void update_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> authorService.update(99L, new AuthorRequest("Имя")));
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — успех, у автора есть комиксы — обнуляются")
    void delete_success_withComics() {
        // Author.comics — поле без сеттера, заполняем через getComics().add()
        Comic comic = new Comic();
        comic.setAuthor(testAuthor);
        testAuthor.getComics().add(comic);

        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));

        authorService.delete(1L);

        assertNull(comic.getAuthor());
        verify(repository).delete(testAuthor);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — успех, у автора нет комиксов")
    void delete_success_noComics() {
        // getComics() возвращает пустой список (инициализирован в Author)
        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));

        authorService.delete(1L);

        verify(repository).delete(testAuthor);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — автор не найден")
    void delete_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> authorService.delete(99L));
    }
}