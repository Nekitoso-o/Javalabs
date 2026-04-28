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
import org.mockito.Spy;
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

    @Spy
    private AuthorMapper mapper = new AuthorMapper();

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
        testAuthor.setName("Кэнтаро Миура");
        testAuthorDto = new AuthorDto(1L, "Кэнтаро Миура");
    }


    @Test
    @DisplayName("getAll — кеш попадание, репозиторий не вызывается")
    void getAll_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class)))
            .thenReturn(List.of(testAuthorDto));

        List<AuthorDto> result = authorService.getAll();

        assertEquals(1, result.size());
        assertEquals("Кэнтаро Миура", result.get(0).name());
        verify(repository, never()).findAll();
        verify(cacheManager, never()).put(any(), any());
    }

    @Test
    @DisplayName("getAll — кеш промах, данные из БД кешируются")
    void getAll_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(List.of(testAuthor));

        List<AuthorDto> result = authorService.getAll();

        assertEquals(1, result.size());
        assertEquals("Кэнтаро Миура", result.get(0).name());
        verify(repository).findAll();
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getAll — пустой список")
    void getAll_empty() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<AuthorDto> result = authorService.getAll();

        assertTrue(result.isEmpty());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }



    @Test
    @DisplayName("getById — кеш попадание")
    void getById_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(testAuthorDto);

        AuthorDto result = authorService.getById(1L);

        assertNotNull(result);
        assertEquals("Кэнтаро Миура", result.name());
        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("getById — кеш промах, успех")
    void getById_cacheMiss_success() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));

        AuthorDto result = authorService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Кэнтаро Миура", result.name());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getById — не найден, выброс ResourceNotFoundException")
    void getById_notFound() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> authorService.getById(99L));
        verify(cacheManager, never()).put(any(), any());
    }



    @Test
    @DisplayName("create — успешное создание, кеш инвалидируется")
    void create_success() {
        AuthorRequest request = new AuthorRequest("Кэнтаро Миура");
        when(repository.save(any(Author.class))).thenReturn(testAuthor);

        AuthorDto result = authorService.create(request);

        assertNotNull(result);
        assertEquals("Кэнтаро Миура", result.name());
        verify(repository).save(any(Author.class));
        verify(cacheManager).invalidate();
    }



    @Test
    @DisplayName("update — успешное обновление")
    void update_success() {
        AuthorRequest request = new AuthorRequest("Новое имя");
        Author updated = new Author();
        updated.setId(1L);
        updated.setName("Новое имя");

        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(repository.save(any(Author.class))).thenReturn(updated);

        AuthorDto result = authorService.update(1L, request);

        assertNotNull(result);
        assertEquals("Новое имя", result.name());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("update — не найден")
    void update_notFound() {
        AuthorRequest request = new AuthorRequest("Новое имя");
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> authorService.update(99L, request));
        verify(repository, never()).save(any());
        verify(cacheManager, never()).invalidate();
    }



    @Test
    @DisplayName("delete — успешное удаление без комиксов")
    void delete_success_noComics() {
        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));

        authorService.delete(1L);

        verify(repository).delete(testAuthor);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — автор обнуляется у связанных комиксов")
    void delete_nullifiesAuthorOnComics() {
        Comic comic = new Comic();
        comic.setId(10L);
        comic.setAuthor(testAuthor);
        testAuthor.getComics().add(comic);

        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));

        authorService.delete(1L);

        assertNull(comic.getAuthor());
        verify(repository).delete(testAuthor);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — не найден")
    void delete_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> authorService.delete(99L));
        verify(repository, never()).delete(any(Author.class));
        verify(cacheManager, never()).invalidate();
    }
}