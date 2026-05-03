package com.example.mangacatalog.service;

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

    private final AuthorMapper mapper = new AuthorMapper();
    private final ApiCacheManager cacheManager = new ApiCacheManager();

    private AuthorService authorService;
    private Author testAuthor;

    @BeforeEach
    void setUp() {
        authorService = new AuthorService(repository, mapper, cacheManager);
        cacheManager.invalidate();

        testAuthor = new Author();
        testAuthor.setId(1L);
        testAuthor.setName("Акира Торияма");
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll — кэш пуст, запрос к БД")
    void getAll_cacheMiss() {
        when(repository.findAll()).thenReturn(List.of(testAuthor));

        List<AuthorDto> result = authorService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Акира Торияма", result.get(0).name());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll — второй вызов берёт из кэша")
    void getAll_secondCall_fromCache() {
        when(repository.findAll()).thenReturn(List.of(testAuthor));

        authorService.getAll();
        authorService.getAll();

        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll — пустой список")
    void getAll_empty() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<AuthorDto> result = authorService.getAll();

        assertTrue(result.isEmpty());
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — успех, результат кэшируется")
    void getById_success() {
        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));

        AuthorDto result = authorService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Акира Торияма", result.name());
    }

    @Test
    @DisplayName("getById — второй вызов из кэша")
    void getById_secondCall_fromCache() {
        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));

        authorService.getById(1L);
        authorService.getById(1L);

        verify(repository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getById — автор не найден")
    void getById_notFound() {
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
        when(repository.save(any(Author.class))).thenReturn(saved);

        AuthorDto result = authorService.create(request);

        assertNotNull(result);
        assertEquals(2L, result.id());
        assertEquals("Масаси Кисимото", result.name());
        verify(repository).save(any(Author.class));
    }

    @Test
    @DisplayName("create — кэш инвалидируется после создания")
    void create_invalidatesCache() {
        when(repository.findAll()).thenReturn(List.of(testAuthor));
        authorService.getAll();

        Author saved = new Author();
        saved.setId(2L);
        saved.setName("Масаси Кисимото");
        when(repository.save(any(Author.class))).thenReturn(saved);
        AuthorRequest request = new AuthorRequest("Масаси Кисимото");
        authorService.create(request);

        when(repository.findAll()).thenReturn(List.of(testAuthor, saved));
        authorService.getAll();
        verify(repository, times(2)).findAll();
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — успех")
    void update_success() {
        AuthorRequest request = new AuthorRequest("Новое Имя");
        Author updated = new Author();
        updated.setId(1L);
        updated.setName("Новое Имя");
        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(repository.save(any(Author.class))).thenReturn(updated);

        AuthorDto result = authorService.update(1L, request);

        assertNotNull(result);
        assertEquals("Новое Имя", result.name());
        verify(repository).save(any(Author.class));
    }

    @Test
    @DisplayName("update — автор не найден")
    void update_notFound() {
        AuthorRequest request = new AuthorRequest("Имя");
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> authorService.update(99L, request));
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — успех, комиксы автора обнуляются")
    void delete_success_withComics() {
        Comic comic = new Comic();
        comic.setAuthor(testAuthor);
        testAuthor.getComics().add(comic);
        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));

        authorService.delete(1L);

        assertNull(comic.getAuthor());
        verify(repository).delete(testAuthor);
    }

    @Test
    @DisplayName("delete — успех, нет комиксов")
    void delete_success_noComics() {
        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));

        authorService.delete(1L);

        verify(repository).delete(testAuthor);
    }

    @Test
    @DisplayName("delete — автор не найден")
    void delete_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> authorService.delete(99L));
    }
    // Не покрыто: update — кэш инвалидируется
    @Test
    @DisplayName("update — кэш инвалидируется")
    void update_invalidatesCache() {
        when(repository.findAll()).thenReturn(List.of(testAuthor));
        authorService.getAll(); // заполняем кэш

        Author updated = new Author();
        updated.setId(1L);
        updated.setName("Новое Имя");
        AuthorRequest request = new AuthorRequest("Новое Имя");
        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(repository.save(any(Author.class))).thenReturn(updated);
        authorService.update(1L, request); // инвалидирует кэш

        when(repository.findAll()).thenReturn(List.of(updated));
        authorService.getAll(); // снова идёт в БД
        verify(repository, times(2)).findAll();
    }

    // Не покрыто: delete — кэш инвалидируется
    @Test
    @DisplayName("delete — кэш инвалидируется")
    void delete_invalidatesCache() {
        when(repository.findAll()).thenReturn(List.of(testAuthor));
        authorService.getAll(); // заполняем кэш

        when(repository.findById(1L)).thenReturn(Optional.of(testAuthor));
        authorService.delete(1L); // инвалидирует кэш

        Author newAuthor = new Author();
        newAuthor.setId(2L);
        newAuthor.setName("Другой");
        when(repository.findAll()).thenReturn(List.of(newAuthor));
        authorService.getAll(); // снова идёт в БД
        verify(repository, times(2)).findAll();
    }

    @Test
    @DisplayName("delete — успех, пустой список комиксов у автора")
    void delete_success_emptyComicsList() {
        Author emptyAuthor = new Author();
        emptyAuthor.setId(3L);
        emptyAuthor.setName("Пустой автор");

        when(repository.findById(3L)).thenReturn(Optional.of(emptyAuthor));

        authorService.delete(3L);

        verify(repository).delete(emptyAuthor);
    }
}