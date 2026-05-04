package com.example.mangacatalog.service;

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

    private final PublisherMapper mapper = new PublisherMapper();
    private final ApiCacheManager cacheManager = new ApiCacheManager();

    private PublisherService publisherService;
    private Publisher testPublisher;

    @BeforeEach
    void setUp() {
        publisherService = new PublisherService(repository, mapper, cacheManager);
        cacheManager.invalidate();

        testPublisher = new Publisher();
        testPublisher.setId(1L);
        testPublisher.setName("Shueisha");
    }


    @Test
    @DisplayName("getAll — кэш пуст, запрос к БД")
    void getAll_cacheMiss() {
        when(repository.findAll()).thenReturn(List.of(testPublisher));

        List<PublisherDto> result = publisherService.getAll();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Shueisha", result.get(0).name());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll — второй вызов из кэша")
    void getAll_secondCall_fromCache() {
        when(repository.findAll()).thenReturn(List.of(testPublisher));

        publisherService.getAll();
        publisherService.getAll();

        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll — пустой список")
    void getAll_empty() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(publisherService.getAll().isEmpty());
    }



    @Test
    @DisplayName("getById — успех")
    void getById_success() {
        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));

        PublisherDto result = publisherService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Shueisha", result.name());
    }

    @Test
    @DisplayName("getById — второй вызов из кэша")
    void getById_secondCall_fromCache() {
        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));

        publisherService.getById(1L);
        publisherService.getById(1L);

        verify(repository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getById — не найден")
    void getById_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> publisherService.getById(99L));
    }



    @Test
    @DisplayName("create — успех")
    void create_success() {
        PublisherRequest request = new PublisherRequest("Kodansha");
        Publisher saved = new Publisher();
        saved.setId(2L);
        saved.setName("Kodansha");
        when(repository.save(any(Publisher.class))).thenReturn(saved);

        PublisherDto result = publisherService.create(request);

        assertNotNull(result);
        assertEquals(2L, result.id());
        assertEquals("Kodansha", result.name());
    }

    @Test
    @DisplayName("create — кэш инвалидируется")
    void create_invalidatesCache() {
        when(repository.findAll()).thenReturn(List.of(testPublisher));
        publisherService.getAll();

        Publisher saved = new Publisher();
        saved.setId(2L);
        saved.setName("Kodansha");
        when(repository.save(any(Publisher.class))).thenReturn(saved);
        publisherService.create(new PublisherRequest("Kodansha"));

        when(repository.findAll()).thenReturn(List.of(testPublisher, saved));
        publisherService.getAll();
        verify(repository, times(2)).findAll();
    }



    @Test
    @DisplayName("update — успех")
    void update_success() {
        PublisherRequest request = new PublisherRequest("Viz Media");
        Publisher updated = new Publisher();
        updated.setId(1L);
        updated.setName("Viz Media");
        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(repository.save(any(Publisher.class))).thenReturn(updated);

        PublisherDto result = publisherService.update(1L, request);

        assertEquals("Viz Media", result.name());
        verify(repository).save(any(Publisher.class));
    }

    @Test
    @DisplayName("update — не найден")
    void update_notFound() {
        PublisherRequest request = new PublisherRequest("Имя");
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> publisherService.update(99L, request));
    }

    @Test
    @DisplayName("update — кэш инвалидируется")
    void update_invalidatesCache() {
        when(repository.findAll()).thenReturn(List.of(testPublisher));
        publisherService.getAll();

        Publisher updated = new Publisher();
        updated.setId(1L);
        updated.setName("Viz Media");
        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(repository.save(any(Publisher.class))).thenReturn(updated);
        publisherService.update(1L, new PublisherRequest("Viz Media"));

        when(repository.findAll()).thenReturn(List.of(updated));
        publisherService.getAll();
        verify(repository, times(2)).findAll();
    }



    @Test
    @DisplayName("delete — успех, комиксы обнуляются")
    void delete_success_withComics() {
        Comic comic = new Comic();
        comic.setPublisher(testPublisher);
        testPublisher.getComics().add(comic);
        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));

        publisherService.delete(1L);

        assertNull(comic.getPublisher());
        verify(repository).delete(testPublisher);
    }

    @Test
    @DisplayName("delete — успех, нет комиксов")
    void delete_success_noComics() {
        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));

        publisherService.delete(1L);

        verify(repository).delete(testPublisher);
    }

    @Test
    @DisplayName("delete — не найден")
    void delete_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> publisherService.delete(99L));
    }

    @Test
    @DisplayName("delete — кэш инвалидируется")
    void delete_invalidatesCache() {
        when(repository.findAll()).thenReturn(List.of(testPublisher));
        publisherService.getAll();

        when(repository.findById(1L)).thenReturn(Optional.of(testPublisher));
        publisherService.delete(1L);

        when(repository.findAll()).thenReturn(Collections.emptyList());
        publisherService.getAll();
        verify(repository, times(2)).findAll();
    }
}