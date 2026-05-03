package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.BulkComicResult;
import com.example.mangacatalog.dto.ComicRequest;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Genre;
import com.example.mangacatalog.entity.Publisher;
import com.example.mangacatalog.mapper.AuthorMapper;
import com.example.mangacatalog.mapper.ComicMapper;
import com.example.mangacatalog.mapper.GenreMapper;
import com.example.mangacatalog.mapper.PublisherMapper;
import com.example.mangacatalog.repository.AuthorRepository;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.GenreRepository;
import com.example.mangacatalog.repository.PublisherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private final ApiCacheManager cacheManager = new ApiCacheManager();
    private final AuthorMapper authorMapper = new AuthorMapper();
    private final PublisherMapper publisherMapper = new PublisherMapper();
    private final GenreMapper genreMapper = new GenreMapper();
    private ComicMapper comicMapper;
    private ComicBulkService comicBulkService;

    private Author testAuthor;
    private Publisher testPublisher;
    private Genre testGenre;
    private Comic savedComic;

    @BeforeEach
    void setUp() {
        comicMapper = new ComicMapper(authorMapper, publisherMapper, genreMapper);
        comicBulkService = new ComicBulkService(
            comicRepository, authorRepository, publisherRepository,
            genreRepository, comicMapper, cacheManager);
        cacheManager.invalidate();

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
        savedComic.setReleaseYear(1989);
        savedComic.setAuthor(testAuthor);
        savedComic.setPublisher(testPublisher);
        savedComic.setGenres(new HashSet<>(Set.of(testGenre)));
    }

    @Test
    @DisplayName("createBulk — один комикс, успех")
    void createBulk_singleComic_success() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(savedComic);

        List<ComicRequest> requests = List.of(request);
        BulkComicResult result = comicBulkService.createBulk(requests);

        assertNotNull(result);
        assertEquals(1, result.successCount());
        assertEquals(1, result.created().size());
        assertEquals("Берсерк", result.created().get(0).title());
        assertEquals("Кэнтаро Миура", result.created().get(0).author().name());
        assertEquals("Hakusensha", result.created().get(0).publisher().name());
    }

    @Test
    @DisplayName("createBulk — несколько комиксов, успех")
    void createBulk_multipleComics_success() {
        ComicRequest req1 = new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of(1L));
        ComicRequest req2 = new ComicRequest("One Piece", 1997, 1L, 1L, Set.of(1L));
        Comic saved2 = new Comic();
        saved2.setId(11L);
        saved2.setTitle("One Piece");
        saved2.setReleaseYear(1997);
        saved2.setAuthor(testAuthor);
        saved2.setPublisher(testPublisher);
        saved2.setGenres(new HashSet<>(Set.of(testGenre)));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class)))
            .thenReturn(savedComic)
            .thenReturn(saved2);

        List<ComicRequest> requests = List.of(req1, req2);
        BulkComicResult result = comicBulkService.createBulk(requests);

        assertEquals(2, result.successCount());
        assertEquals(2, result.created().size());
        verify(comicRepository, times(2)).save(any(Comic.class));
    }

    @Test
    @DisplayName("createBulk — автор не найден")
    void createBulk_authorNotFound() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 99L, 1L, Set.of(1L));
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        List<ComicRequest> requests = List.of(request);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(requests));

        assertTrue(ex.getMessage().contains("Элемент [0]"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — издатель не найден")
    void createBulk_publisherNotFound() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 99L, Set.of(1L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(99L)).thenReturn(Optional.empty());

        List<ComicRequest> requests = List.of(request);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(requests));

        assertTrue(ex.getMessage().contains("Элемент [0]"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — жанр не найден")
    void createBulk_genreNotFound() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 1L, Set.of(99L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(99L))).thenReturn(Collections.emptyList());

        List<ComicRequest> requests = List.of(request);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(requests));

        assertTrue(ex.getMessage().contains("Элемент [0]"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — пустые жанры")
    void createBulk_emptyGenres() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 1L, Collections.emptySet());
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));

        List<ComicRequest> requests = List.of(request);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(requests));

        assertTrue(ex.getMessage().contains("список жанров не может быть пустым"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — жанры найдены частично")
    void createBulk_partialGenresFound() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 1L, Set.of(1L, 2L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(testGenre));

        List<ComicRequest> requests = List.of(request);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(requests));

        assertTrue(ex.getMessage().contains("не найдены"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — ошибка на втором элементе, индекс [1] в сообщении")
    void createBulk_secondElementFails() {
        ComicRequest req1 = new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of(1L));
        ComicRequest req2 = new ComicRequest("Тест", 2020, 99L, 1L, Set.of(1L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(savedComic);

        List<ComicRequest> requests = List.of(req1, req2);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(requests));

        assertTrue(ex.getMessage().contains("Элемент [1]"));
    }
}