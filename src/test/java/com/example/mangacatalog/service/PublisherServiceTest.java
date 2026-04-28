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
import org.mockito.Spy;
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

    @Spy
    private PublisherMapper mapper = new PublisherMapper();

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



    @Test
    @DisplayName("getAll — кеш попадание")
    void getAll_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class)))
            .thenReturn(List.of(testPublisherDto));

        List<PublisherDto> result = publisherService.getAll();

        assertEquals(1, result.size());
        verify(repository, never()).findAll();
    }

    @Test
    @DisplayName("getAll — кеш промах")
    void getAll_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(List.of(testPublisher));

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



    @Test
    @DisplayName("getById — кеш попадание")
    void getById_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(testPublisherDto);

        PublisherDto result = publisherService.getById(1L);

        assertNotNull(result);
        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("getById — кеш промах, успех")
    void getById_cacheMiss_success() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));

        PublisherDto result = publisherService.getById(1L);

        assertNotNull(result);
        assertEquals("Shueisha", result.name());
    }

    @Test
    @DisplayName("getById — не найден")
    void getById_notFound() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> publisherService.getById(99L));
    }



    @Test
    @DisplayName("create — успешное создание")
    void create_success() {
        PublisherRequest request = new PublisherRequest("Shueisha");
        when(repository.save(any(Publisher.class))).thenReturn(testPublisher);

        PublisherDto result = publisherService.create(request);

        assertNotNull(result);
        assertEquals("Shueisha", result.name());
        verify(cacheManager).invalidate();
    }


    @Test
    @DisplayName("update — успешное обновление")
    void update_success() {
        PublisherRequest request = new PublisherRequest("Kodansha");
        Publisher updated = new Publisher();
        updated.setId(1L);
        updated.setName("Kodansha");

        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(repository.save(any(Publisher.class))).thenReturn(updated);

        PublisherDto result = publisherService.update(1L, request);

        assertEquals("Kodansha", result.name());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("update — не найден")
    void update_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> publisherService.update(99L, new PublisherRequest("X")));
        verify(repository, never()).save(any());
    }


    @Test
    @DisplayName("delete — успешное удаление без комиксов")
    void delete_success_noComics() {
        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));

        publisherService.delete(1L);

        verify(repository).delete(testPublisher);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — publisher обнуляется у комиксов")
    void delete_nullifiesPublisherOnComics() {
        Comic comic = new Comic();
        comic.setId(10L);
        comic.setPublisher(testPublisher);
        testPublisher.getComics().add(comic);

        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));

        publisherService.delete(1L);

        assertNull(comic.getPublisher());
        verify(repository).delete(testPublisher);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — не найден")
    void delete_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> publisherService.delete(99L));
        verify(repository, never()).delete(any(Publisher.class));
    }
}