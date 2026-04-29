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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private Publisher publisher;
    private PublisherDto publisherDto;

    @BeforeEach
    void setUp() {
        publisher = new Publisher();
        publisher.setId(1L);
        publisher.setName("Test Publisher");

        publisherDto = new PublisherDto(1L, "Test Publisher");
    }

    @Test
    void getAll_whenCacheHit_returnsCached() {
        List<PublisherDto> cached = List.of(publisherDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<PublisherDto> result = publisherService.getAll();

        assertThat(result).isEqualTo(cached);
        verify(repository, never()).findAll();
    }

    @Test
    void getAll_whenCacheMiss_fetchesAndCaches() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(List.of(publisher));
        when(mapper.toDto(publisher)).thenReturn(publisherDto);

        List<PublisherDto> result = publisherService.getAll();

        assertThat(result).containsExactly(publisherDto);
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    void getById_whenCacheHit_returnsCached() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(publisherDto);

        PublisherDto result = publisherService.getById(1L);

        assertThat(result).isEqualTo(publisherDto);
        verify(repository, never()).findById(any());
    }

    @Test
    void getById_whenCacheMiss_fetchesFromDb() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(1L)).thenReturn(Optional.of(publisher));
        when(mapper.toDto(publisher)).thenReturn(publisherDto);

        PublisherDto result = publisherService.getById(1L);

        assertThat(result).isEqualTo(publisherDto);
    }

    @Test
    void getById_whenNotFound_throwsException() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publisherService.getById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    void create_savesAndInvalidates() {
        PublisherRequest request = new PublisherRequest("New Pub");
        Publisher saved = new Publisher();
        saved.setId(2L);
        saved.setName("New Pub");
        PublisherDto savedDto = new PublisherDto(2L, "New Pub");

        when(repository.save(any(Publisher.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(savedDto);

        PublisherDto result = publisherService.create(request);

        assertThat(result).isEqualTo(savedDto);
        verify(cacheManager).invalidate();
    }

    @Test
    void update_whenFound_updatesAndInvalidates() {
        PublisherRequest request = new PublisherRequest("Updated");
        Publisher updated = new Publisher();
        updated.setId(1L);
        updated.setName("Updated");
        PublisherDto updatedDto = new PublisherDto(1L, "Updated");

        when(repository.findById(1L)).thenReturn(Optional.of(publisher));
        when(repository.save(any(Publisher.class))).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(updatedDto);

        PublisherDto result = publisherService.update(1L, request);

        assertThat(result).isEqualTo(updatedDto);
        verify(cacheManager).invalidate();
    }

    @Test
    void update_whenNotFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publisherService.update(99L, new PublisherRequest("x")))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_whenPublisherHasNoComics_deletesAndInvalidates() {
        when(repository.findById(1L)).thenReturn(Optional.of(publisher));

        publisherService.delete(1L);

        verify(repository).delete(publisher);
        verify(cacheManager).invalidate();
    }

    @Test
    void delete_whenPublisherHasComics_nullifiesPublisherOnComics() {
        Publisher publisherWithComics = spy(new Publisher());
        publisherWithComics.setId(1L);
        publisherWithComics.setName("Pub");

        Comic comic1 = new Comic();
        comic1.setPublisher(publisherWithComics);
        Comic comic2 = new Comic();
        comic2.setPublisher(publisherWithComics);

        List<Comic> comics = new ArrayList<>();
        comics.add(comic1);
        comics.add(comic2);
        when(publisherWithComics.getComics()).thenReturn(comics);

        when(repository.findById(1L)).thenReturn(Optional.of(publisherWithComics));

        publisherService.delete(1L);

        assertThat(comic1.getPublisher()).isNull();
        assertThat(comic2.getPublisher()).isNull();
        verify(repository).delete(publisherWithComics);
        verify(cacheManager).invalidate();
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publisherService.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}