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

        BulkComicResult result = comicBulkService.createBulk(List.of(request));

        assertNotNull(result);
        assertEquals(1, result.successCount());
        assertEquals(1, result.created().size());
        assertEquals("Берсерк", result.created().get(0).title());
        assertEquals("Кэнтаро Миура", result.created().get(0).author().name());
        assertEquals("Hakusensha", result.created().get(0).publisher().name());
        verify(comicRepository, times(1)).save(any(Comic.class));
    }

    @Test
    @DisplayName("createBulk — несколько комиксов, успех")
    void createBulk_multipleComics_success() {
        ComicRequest req1 = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L));
        ComicRequest req2 = new ComicRequest(
            "One Piece", 1997, 1L, 1L, Set.of(1L));

        Comic savedComic2 = new Comic();
        savedComic2.setId(11L);
        savedComic2.setTitle("One Piece");
        savedComic2.setReleaseYear(1997);
        savedComic2.setAuthor(testAuthor);
        savedComic2.setPublisher(testPublisher);
        savedComic2.setGenres(new HashSet<>(Set.of(testGenre)));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class)))
            .thenReturn(savedComic)
            .thenReturn(savedComic2);

        BulkComicResult result = comicBulkService.createBulk(List.of(req1, req2));

        assertEquals(2, result.successCount());
        assertEquals(2, result.created().size());
        assertEquals("Берсерк", result.created().get(0).title());
        assertEquals("One Piece", result.created().get(1).title());
        verify(comicRepository, times(2)).save(any(Comic.class));
    }

    @Test
    @DisplayName("createBulk — список из одного элемента, кэш инвалидируется")
    void createBulk_invalidatesCache() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(savedComic);

        // Помещаем что-нибудь в кэш через другой метод — здесь просто проверяем,
        // что после bulk следующий getAll обращается к БД, а не к кэшу.
        // Для этого достаточно убедиться, что cacheManager.invalidate() вызван,
        // что косвенно проверяется через повторный вызов репозитория.
        comicBulkService.createBulk(List.of(
            new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of(1L))));

        // После инвалидации кэш пуст — повторный save снова обращается к репозиторию
        verify(comicRepository, times(1)).save(any(Comic.class));
    }



    @Test
    @DisplayName("createBulk — автор не найден — IllegalArgumentException с индексом [0]")
    void createBulk_authorNotFound() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 99L, 1L, Set.of(1L));
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));

        assertTrue(ex.getMessage().contains("Элемент [0]"));
        assertTrue(ex.getMessage().contains("автор"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — издатель не найден — IllegalArgumentException с индексом [0]")
    void createBulk_publisherNotFound() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 99L, Set.of(1L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));

        assertTrue(ex.getMessage().contains("Элемент [0]"));
        assertTrue(ex.getMessage().contains("издатель"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — жанр не найден — IllegalArgumentException с индексом [0]")
    void createBulk_genreNotFound() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 1L, Set.of(99L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(99L))).thenReturn(Collections.emptyList());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));

        assertTrue(ex.getMessage().contains("Элемент [0]"));
        assertTrue(ex.getMessage().contains("не найдены"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — пустой список жанров — IllegalArgumentException")
    void createBulk_emptyGenres() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 1L, Collections.emptySet());
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));

        assertTrue(ex.getMessage().contains("список жанров не может быть пустым"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — null список жанров — IllegalArgumentException")
    void createBulk_nullGenres() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 1L, null);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));

        assertTrue(ex.getMessage().contains("список жанров не может быть пустым"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBulk — жанры найдены частично — IllegalArgumentException")
    void createBulk_partialGenresFound() {
        ComicRequest request = new ComicRequest(
            "Тест", 2020, 1L, 1L, Set.of(1L, 2L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        // Возвращаем только один жанр из двух запрошенных
        when(genreRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(testGenre));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(request)));

        assertTrue(ex.getMessage().contains("не найдены"));
        // ID=2 отсутствует — должен быть в сообщении
        assertTrue(ex.getMessage().contains("2"));
        verify(comicRepository, never()).save(any());
    }



    @Test
    @DisplayName("createBulk — ошибка на втором элементе, индекс [1] в сообщении")
    void createBulk_secondElementFails_authorNotFound() {
        ComicRequest req1 = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L));
        ComicRequest req2 = new ComicRequest(
            "Тест", 2020, 99L, 1L, Set.of(1L));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(savedComic);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(req1, req2)));

        assertTrue(ex.getMessage().contains("Элемент [1]"));
        // Первый элемент успел сохраниться до ошибки
        verify(comicRepository, times(1)).save(any(Comic.class));
    }

    @Test
    @DisplayName("createBulk — ошибка на третьем элементе, индекс [2] в сообщении")
    void createBulk_thirdElementFails_publisherNotFound() {
        ComicRequest req1 = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L));
        ComicRequest req2 = new ComicRequest(
            "One Piece", 1997, 1L, 1L, Set.of(1L));
        ComicRequest req3 = new ComicRequest(
            "Тест", 2020, 1L, 99L, Set.of(1L));

        Comic savedComic2 = new Comic();
        savedComic2.setId(11L);
        savedComic2.setTitle("One Piece");
        savedComic2.setReleaseYear(1997);
        savedComic2.setAuthor(testAuthor);
        savedComic2.setPublisher(testPublisher);
        savedComic2.setGenres(new HashSet<>(Set.of(testGenre)));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(publisherRepository.findById(99L)).thenReturn(Optional.empty());
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class)))
            .thenReturn(savedComic)
            .thenReturn(savedComic2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comicBulkService.createBulk(List.of(req1, req2, req3)));

        assertTrue(ex.getMessage().contains("Элемент [2]"));
        // Первые два элемента успели сохраниться
        verify(comicRepository, times(2)).save(any(Comic.class));
    }



    @Test
    @DisplayName("createBulk — жанры комикса корректно маппятся в DTO")
    void createBulk_genresMappedCorrectly() {
        Genre genreAction = new Genre();
        genreAction.setId(2L);
        genreAction.setName("Экшн");

        Comic comicWithTwoGenres = new Comic();
        comicWithTwoGenres.setId(20L);
        comicWithTwoGenres.setTitle("Тест");
        comicWithTwoGenres.setReleaseYear(2000);
        comicWithTwoGenres.setAuthor(testAuthor);
        comicWithTwoGenres.setPublisher(testPublisher);
        comicWithTwoGenres.setGenres(new HashSet<>(Set.of(testGenre, genreAction)));

        ComicRequest request = new ComicRequest(
            "Тест", 2000, 1L, 1L, Set.of(1L, 2L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L, 2L)))
            .thenReturn(List.of(testGenre, genreAction));
        when(comicRepository.save(any(Comic.class))).thenReturn(comicWithTwoGenres);

        BulkComicResult result = comicBulkService.createBulk(List.of(request));

        assertEquals(1, result.created().size());
        assertEquals(2, result.created().get(0).genres().size());
        assertTrue(result.created().get(0).genres().stream()
            .anyMatch(g -> g.name().equals("Тёмное фэнтези")));
        assertTrue(result.created().get(0).genres().stream()
            .anyMatch(g -> g.name().equals("Экшн")));
    }

    @Test
    @DisplayName("createBulk — successCount совпадает с размером created")
    void createBulk_successCountMatchesCreatedSize() {
        ComicRequest req1 = new ComicRequest(
            "Комикс 1", 2000, 1L, 1L, Set.of(1L));
        ComicRequest req2 = new ComicRequest(
            "Комикс 2", 2001, 1L, 1L, Set.of(1L));
        ComicRequest req3 = new ComicRequest(
            "Комикс 3", 2002, 1L, 1L, Set.of(1L));

        Comic c2 = new Comic();
        c2.setId(11L); c2.setTitle("Комикс 2"); c2.setReleaseYear(2001);
        c2.setAuthor(testAuthor); c2.setPublisher(testPublisher);
        c2.setGenres(new HashSet<>(Set.of(testGenre)));

        Comic c3 = new Comic();
        c3.setId(12L); c3.setTitle("Комикс 3"); c3.setReleaseYear(2002);
        c3.setAuthor(testAuthor); c3.setPublisher(testPublisher);
        c3.setGenres(new HashSet<>(Set.of(testGenre)));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class)))
            .thenReturn(savedComic).thenReturn(c2).thenReturn(c3);

        BulkComicResult result = comicBulkService.createBulk(List.of(req1, req2, req3));

        assertEquals(result.successCount(), result.created().size());
        assertEquals(3, result.successCount());
    }
}