package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.BulkComicResult;
import com.example.mangacatalog.dto.ComicDto;
import com.example.mangacatalog.dto.ComicRequest;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Genre;
import com.example.mangacatalog.entity.Publisher;
import com.example.mangacatalog.mapper.ComicMapper;
import com.example.mangacatalog.repository.AuthorRepository;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.GenreRepository;
import com.example.mangacatalog.repository.PublisherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComicBulkServiceTest {

    @Mock
    private ComicRepository comicRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private PublisherRepository publisherRepository;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private ComicMapper comicMapper;
    @Mock
    private ApiCacheManager cacheManager;

    @InjectMocks
    private ComicBulkService comicBulkService;

    private Author testAuthor;
    private Publisher testPublisher;
    private Genre testGenre;
    private Comic savedComic;
    private ComicDto testComicDto;

    @BeforeEach
    void setUp() {
        testAuthor = new Author();
        testAuthor.setId(1L);
        testAuthor.setName("Кэнтаро Миура");

        testPublisher = new Publisher();
        testPublisher.setId(1L);
        testPublisher.setName("Hakusensha");

        testGenre = new Genre();
        testGenre.setId(1L);
        testGenre.setName("Тёмное фэнтези");

        savedComic = new Comic();
        savedComic.setId(10L);
        savedComic.setTitle("Берсерк");

        testComicDto = new ComicDto(
            10L, "Берсерк", 1989,
            null, null, Set.of()
        );
    }

    @Test
    @DisplayName("createBulk — успех, один комикс")
    void createBulk_singleComic_success() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(savedComic);
        when(comicMapper.toDto(savedComic)).thenReturn(testComicDto);

        BulkComicResult result = comicBulkService.createBulk(List.of(request));

        assertNotNull(result);
        assertEquals(1, result.successCount());
        assertEquals(1, result.created().size());
        assertEquals("Берсерк", result.created().get(0).title());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("createBulk — успех, несколько комиксов")
    void createBulk_multipleComics_success() {
        ComicRequest req1 = new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of(1L));
        ComicRequest req2 = new ComicRequest("One Piece", 1997, 1L, 1L, Set.of(1L));

        Comic saved2 = new Comic();
        saved2.setId(11L);
        saved2.setTitle("One Piece");
        ComicDto dto2 = new ComicDto(11L, "One Piece", 1997, null, null, Set.of());

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class)))
            .thenReturn(savedComic)
            .thenReturn(saved2);
        when(comicMapper.toDto(savedComic)).thenReturn(testComicDto);
        when(comicMapper.toDto(saved2)).thenReturn(dto2);

        BulkComicResult result = comicBulkService.createBulk(List.of(req1, req2));

        assertEquals(2, result.successCount());
        assertEquals(2, result.created().size());
        verify(comicRepository, times(2)).save(any(Comic.class));
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("createBulk — автор не найден, бросает IllegalArgumentException")
    void createBulk_authorNotFound() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 99L, 1L, Set.of(1L));

        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));

        assertTrue(ex.getMessage().contains("Элемент [0]"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — издатель не найден, бросает IllegalArgumentException")
    void createBulk_publisherNotFound() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 99L, Set.of(1L));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));

        assertTrue(ex.getMessage().contains("Элемент [0]"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — жанр не найден, бросает IllegalArgumentException")
    void createBulk_genreNotFound() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 1L, Set.of(99L));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(99L))).thenReturn(Collections.emptyList());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));

        assertTrue(ex.getMessage().contains("Элемент [0]"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — пустые жанры, бросает IllegalArgumentException")
    void createBulk_emptyGenres() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 1L, Collections.emptySet());

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));

        assertTrue(ex.getMessage().contains("список жанров не может быть пустым"));
    }

    @Test
    @DisplayName("createBulk — второй элемент невалиден, первый не сохраняется (транзакция)")
    void createBulk_secondElementFails() {
        ComicRequest req1 = new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of(1L));
        ComicRequest req2 = new ComicRequest("Тест", 2020, 99L, 1L, Set.of(1L));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(savedComic);
        when(comicMapper.toDto(savedComic)).thenReturn(testComicDto);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(req1, req2)));

        assertTrue(ex.getMessage().contains("Элемент [1]"));
    }

    @Test
    @DisplayName("createBulk — несколько жанров найдены частично")
    void createBulk_partialGenresFound() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 1L, Set.of(1L, 2L));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        // Запрошены 2 жанра, найден только 1
        when(genreRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(testGenre));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));

        assertTrue(ex.getMessage().contains("не найдены"));
        verify(comicRepository, never()).save(any());
    }
}