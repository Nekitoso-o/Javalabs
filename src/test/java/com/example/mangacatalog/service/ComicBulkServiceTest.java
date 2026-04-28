package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.BulkComicResult;
import com.example.mangacatalog.dto.ComicDto;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private ApiCacheManager cacheManager;

    @Spy
    private ComicMapper comicMapper = new ComicMapper(
        new AuthorMapper(),
        new PublisherMapper(),
        new GenreMapper()
    );

    @InjectMocks
    private ComicBulkService comicBulkService;

    private Author testAuthor;
    private Publisher testPublisher;
    private Genre testGenre;
    private Comic savedComic;

    @BeforeEach
    void setUp() {
        testAuthor = new Author();
        testAuthor.setId(1L);
        testAuthor.setName("Миура");

        testPublisher = new Publisher();
        testPublisher.setId(1L);
        testPublisher.setName("Shueisha");

        testGenre = new Genre();
        testGenre.setId(1L);
        testGenre.setName("Сёнэн");

        savedComic = new Comic();
        savedComic.setId(10L);
        savedComic.setTitle("Берсерк");
        savedComic.setReleaseYear(1989);
        savedComic.setAuthor(testAuthor);
        savedComic.setPublisher(testPublisher);
        savedComic.getGenres().add(testGenre);
    }

    @Test
    @DisplayName("createBulk — успешное создание одного комикса")
    void createBulk_singleSuccess() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(savedComic);

        BulkComicResult result = comicBulkService.createBulk(List.of(request));

        assertEquals(1, result.successCount());
        assertEquals(1, result.created().size());
        verify(comicRepository, times(1)).save(any(Comic.class));
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("createBulk — успешное создание нескольких комиксов")
    void createBulk_multipleSuccess() {
        ComicRequest req1 = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L));
        ComicRequest req2 = new ComicRequest(
            "Наруто", 1999, 1L, 1L, Set.of(1L));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(savedComic);

        BulkComicResult result = comicBulkService.createBulk(List.of(req1, req2));

        assertEquals(2, result.successCount());
        verify(comicRepository, times(2)).save(any(Comic.class));
        verify(cacheManager, times(1)).invalidate();
    }

    @Test
    @DisplayName("createBulk — автор не найден, выброс исключения")
    void createBulk_authorNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 99L, 1L, Set.of(1L));
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));
        verify(comicRepository, never()).save(any());
        verify(cacheManager, never()).invalidate();
    }

    @Test
    @DisplayName("createBulk — издатель не найден, выброс исключения")
    void createBulk_publisherNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 99L, Set.of(1L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — часть жанров не найдена, выброс исключения")
    void createBulk_genresPartiallyNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L, 999L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L, 999L)))
            .thenReturn(List.of(testGenre));

        assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — пустой список жанров, выброс исключения")
    void createBulk_emptyGenres() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of());
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));

        assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — кеш инвалидируется ровно один раз")
    void createBulk_cacheInvalidatedOnce() {
        ComicRequest req = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(savedComic);

        comicBulkService.createBulk(List.of(req, req));

        verify(cacheManager, times(1)).invalidate();
    }

    @Test
    @DisplayName("createBulk — сообщение ошибки содержит индекс элемента")
    void createBulk_errorMessageContainsIndex() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 99L, 1L, Set.of(1L));
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));

        assertTrue(ex.getMessage().contains("[0]"));
    }
}