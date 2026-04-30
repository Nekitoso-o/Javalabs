package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.PublisherDto;
import com.example.mangacatalog.dto.PublisherRequest;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Publisher;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.mapper.PublisherMapper;
import com.example.mangacatalog.repository.PublisherRepository;
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
class PublisherServiceTest {

    @Mock
    private PublisherRepository repository;
    @Mock
    private PublisherMapper mapper;
    @Mock
    private ApiCacheManager cacheManager;

    @InjectMocks
    private PublisherService publisherService;

    private Publisher testPublisher;
    private PublisherDto testPublisherDto;

    @BeforeEach
    void setUp() {
        testPublisher = new Publisher();
        testPublisher.setId(1L);
        testPublisher.setName("Shueisha");
        testPublisherDto = new PublisherDto(1L, "Shueisha");
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll — из кэша")
    void getAll_fromCache() {
        List<PublisherDto> cached = List.of(testPublisherDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<PublisherDto> result = publisherService.getAll();

        assertEquals(cached, result);
        verify(repository, never()).findAll();
    }

    @Test
    @DisplayName("getAll — кэш пуст, запрос к БД")
    void getAll_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(List.of(testPublisher));
        when(mapper.toDto(testPublisher)).thenReturn(testPublisherDto);

        List<PublisherDto> result = publisherService.getAll();

        assertEquals(1, result.size());
        assertEquals("Shueisha", result.get(0).name());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getAll — пустой список")
    void getAll_empty() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(publisherService.getAll().isEmpty());
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — из кэша")
    void getById_fromCache() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(testPublisherDto);

        PublisherDto result = publisherService.getById(1L);

        assertEquals(testPublisherDto, result);
        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("getById — кэш пуст, успех")
    void getById_cacheMiss_success() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(mapper.toDto(testPublisher)).thenReturn(testPublisherDto);

        PublisherDto result = publisherService.getById(1L);

        assertNotNull(result);
        assertEquals("Shueisha", result.name());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getById — издатель не найден")
    void getById_notFound() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> publisherService.getById(99L));
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — успех")
    void create_success() {
        PublisherRequest request = new PublisherRequest("Kodansha");
        Publisher saved = new Publisher();
        saved.setId(2L);
        saved.setName("Kodansha");
        PublisherDto savedDto = new PublisherDto(2L, "Kodansha");

        when(repository.save(any(Publisher.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(savedDto);

        PublisherDto result = publisherService.create(request);

        assertNotNull(result);
        assertEquals("Kodansha", result.name());
        verify(cacheManager).invalidate();
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — успех")
    void update_success() {
        PublisherRequest request = new PublisherRequest("Viz Media");
        PublisherDto updatedDto = new PublisherDto(1L, "Viz Media");

        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(repository.save(any(Publisher.class))).thenReturn(testPublisher);
        when(mapper.toDto(testPublisher)).thenReturn(updatedDto);

        PublisherDto result = publisherService.update(1L, request);

        assertEquals("Viz Media", result.name());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("update — издатель не найден")
    void update_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> publisherService.update(99L, new PublisherRequest("Имя")));
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — успех, комиксы обнуляются")
    void delete_success_withComics() {
        // Publisher.comics — без сеттера, заполняем через getComics().add()
        Comic comic = new Comic();
        comic.setPublisher(testPublisher);
        testPublisher.getComics().add(comic);

        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));

        publisherService.delete(1L);

        assertNull(comic.getPublisher());
        verify(repository).delete(testPublisher);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — издатель без комиксов")
    void delete_success_noComics() {
        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));

        publisherService.delete(1L);

        verify(repository).delete(testPublisher);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — издатель не найден")
    void delete_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> publisherService.delete(99L));
    }
}