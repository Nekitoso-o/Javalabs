
package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.*;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Genre;
import com.example.mangacatalog.entity.Publisher;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.exception.ValidationException;
import com.example.mangacatalog.mapper.AuthorMapper;
import com.example.mangacatalog.mapper.ComicMapper;
import com.example.mangacatalog.mapper.GenreMapper;
import com.example.mangacatalog.mapper.PublisherMapper;
import com.example.mangacatalog.repository.AuthorRepository;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.GenreRepository;
import com.example.mangacatalog.repository.PublisherRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComicServiceTest {

    @Mock
    private ComicRepository comicRepository;
    @Mock
    private PublisherRepository publisherRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private GenreRepository genreRepository;

    private final ApiCacheManager cacheManager = new ApiCacheManager();
    private final AuthorMapper authorMapper = new AuthorMapper();
    private final PublisherMapper publisherMapper = new PublisherMapper();
    private final GenreMapper genreMapper = new GenreMapper();
    private ComicMapper comicMapper;
    private Validator validator;
    private ComicService comicService;

    private Author testAuthor;
    private Publisher testPublisher;
    private Genre testGenre;
    private Comic testComic;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        comicMapper = new ComicMapper(authorMapper, publisherMapper, genreMapper);
        comicService = new ComicService(
            comicRepository, publisherRepository, authorRepository,
            genreRepository, comicMapper, cacheManager, validator);
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

        testComic = new Comic();
        testComic.setId(1L);
        testComic.setTitle("Берсерк");
        testComic.setReleaseYear(1989);
        testComic.setAuthor(testAuthor);
        testComic.setPublisher(testPublisher);
        testComic.setGenres(new HashSet<>(Set.of(testGenre)));
    }

// ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll — кэш пуст, запрос к БД")
    void getAll_cacheMiss() {
        when(comicRepository.findAll()).thenReturn(List.of(testComic));

        List<ComicDto> result = comicService.getAll();

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
        assertEquals(1989, result.get(0).releaseYear());
        assertEquals("Кэнтаро Миура", result.get(0).author().name());
        verify(comicRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll — второй вызов из кэша")
    void getAll_secondCall_fromCache() {
        when(comicRepository.findAll()).thenReturn(List.of(testComic));

        comicService.getAll();
        comicService.getAll();

        verify(comicRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll — пустой список")
    void getAll_empty() {
        when(comicRepository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(comicService.getAll().isEmpty());
    }

// ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — успех")
    void getById_success() {
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));

        ComicDto result = comicService.getById(1L);

        assertNotNull(result);
        assertEquals("Берсерк", result.title());
        assertEquals("Кэнтаро Миура", result.author().name());
        assertEquals("Hakusensha", result.publisher().name());
    }

    @Test
    @DisplayName("getById — второй вызов из кэша")
    void getById_secondCall_fromCache() {
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));

        comicService.getById(1L);
        comicService.getById(1L);

        verify(comicRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getById — не найден")
    void getById_notFound() {
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> comicService.getById(99L));
    }

// ─── searchByTitle ────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchByTitle — найдены результаты")
    void searchByTitle_found() {
        when(comicRepository.findByTitleContainingIgnoreCase("Берс"))
            .thenReturn(List.of(testComic));

        List<ComicDto> result = comicService.searchByTitle("Берс");

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
    }

    @Test
    @DisplayName("searchByTitle — второй вызов из кэша")
    void searchByTitle_secondCall_fromCache() {
        when(comicRepository.findByTitleContainingIgnoreCase("Берс"))
            .thenReturn(List.of(testComic));

        comicService.searchByTitle("Берс");
        comicService.searchByTitle("Берс");

        verify(comicRepository, times(1))
            .findByTitleContainingIgnoreCase("Берс");
    }

    @Test
    @DisplayName("searchByTitle — ничего не найдено")
    void searchByTitle_empty() {
        when(comicRepository.findByTitleContainingIgnoreCase("xyz"))
            .thenReturn(Collections.emptyList());

        assertTrue(comicService.searchByTitle("xyz").isEmpty());
    }

// ─── getComicsByAuthor ────────────────────────────────────────────────────

    @Test
    @DisplayName("getComicsByAuthor — успех")
    void getComicsByAuthor_success() {
        when(comicRepository.findByAuthorId(1L)).thenReturn(List.of(testComic));

        List<ComicDto> result = comicService.getComicsByAuthor(1L);

        assertEquals(1, result.size());
        assertEquals("Кэнтаро Миура", result.get(0).author().name());
    }

    @Test
    @DisplayName("getComicsByAuthor — второй вызов из кэша")
    void getComicsByAuthor_secondCall_fromCache() {
        when(comicRepository.findByAuthorId(1L)).thenReturn(List.of(testComic));

        comicService.getComicsByAuthor(1L);
        comicService.getComicsByAuthor(1L);

        verify(comicRepository, times(1)).findByAuthorId(1L);
    }

// ─── searchComplex ────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchComplex — JPQL, кэш пуст")
    void searchComplex_jpql() {
        when(comicRepository.findByGenreAndYearJpql(
            eq("Сёнэн"), eq(2000), any(Pageable.class)))
            .thenReturn(List.of(testComic));

        List<ComicDto> result = comicService.searchComplex("Сёнэн", 2000, 0, 5, false);

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
    }

    @Test
    @DisplayName("searchComplex — JPQL, второй вызов из кэша")
    void searchComplex_jpql_secondCall_fromCache() {
        when(comicRepository.findByGenreAndYearJpql(
            eq("Сёнэн"), eq(2000), any(Pageable.class)))
            .thenReturn(List.of(testComic));

        comicService.searchComplex("Сёнэн", 2000, 0, 5, false);
        comicService.searchComplex("Сёнэн", 2000, 0, 5, false);

        verify(comicRepository, times(1))
            .findByGenreAndYearJpql(any(), any(), any());
    }

    @Test
    @DisplayName("searchComplex — JPQL, пустой результат")
    void searchComplex_jpql_empty() {
        when(comicRepository.findByGenreAndYearJpql(any(), any(), any()))
            .thenReturn(Collections.emptyList());

        List<ComicDto> result = comicService.searchComplex("Жанр", 1999, 0, 5, false);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("searchComplex — Native, с данными")
    void searchComplex_native() {
        ComicNativeProjection proj = new ComicNativeProjection() {
            @Override public Long getId() { return 1L; }
            @Override public String getTitle() { return "Берсерк"; }
            @Override public Integer getReleaseYear() { return 1989; }
            @Override public Long getAuthorId() { return 1L; }
            @Override public String getAuthorName() { return "Кэнтаро Миура"; }
            @Override public Long getPublisherId() { return 1L; }
            @Override public String getPublisherName() { return "Hakusensha"; }
            @Override public String getGenreIds() { return "1"; }
            @Override public String getGenreNames() { return "Тёмное фэнтези"; }
        };

        when(comicRepository.findByGenreAndYearNative(
            eq("Тёмное фэнтези"), eq(1989), any(Pageable.class)))
            .thenReturn(List.of(proj));

        List<ComicDto> result =
            comicService.searchComplex("Тёмное фэнтези", 1989, 0, 5, true);

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
        assertEquals("Кэнтаро Миура", result.get(0).author().name());
        assertEquals("Тёмное фэнтези",
            result.get(0).genres().iterator().next().name());
    }

    @Test
    @DisplayName("searchComplex — Native, несколько жанров")
    void searchComplex_native_multipleGenres() {
        ComicNativeProjection proj = new ComicNativeProjection() {
            @Override public Long getId() { return 1L; }
            @Override public String getTitle() { return "Берсерк"; }
            @Override public Integer getReleaseYear() { return 1989; }
            @Override public Long getAuthorId() { return 1L; }
            @Override public String getAuthorName() { return "Кэнтаро Миура"; }
            @Override public Long getPublisherId() { return 1L; }
            @Override public String getPublisherName() { return "Hakusensha"; }
            @Override public String getGenreIds() { return "1,2"; }
            @Override public String getGenreNames() { return "Фэнтези,Экшн"; }
        };

        when(comicRepository.findByGenreAndYearNative(any(), any(), any()))
            .thenReturn(List.of(proj));

        List<ComicDto> result =
            comicService.searchComplex("Фэнтези", 1989, 0, 5, true);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).genres().size());
    }

    @Test
    @DisplayName("searchComplex — Native, null поля")
    void searchComplex_native_nullFields() {
        ComicNativeProjection proj = new ComicNativeProjection() {
            @Override public Long getId() { return 2L; }
            @Override public String getTitle() { return "Без данных"; }
            @Override public Integer getReleaseYear() { return 2000; }
            @Override public Long getAuthorId() { return null; }
            @Override public String getAuthorName() { return null; }
            @Override public Long getPublisherId() { return null; }
            @Override public String getPublisherName() { return null; }
            @Override public String getGenreIds() { return null; }
            @Override public String getGenreNames() { return null; }
        };

        when(comicRepository.findByGenreAndYearNative(any(), any(), any()))
            .thenReturn(List.of(proj));

        List<ComicDto> result =
            comicService.searchComplex("Жанр", 1999, 0, 5, true);

        assertEquals(1, result.size());
        assertNull(result.get(0).author());
        assertNull(result.get(0).publisher());
        assertTrue(result.get(0).genres().isEmpty());
    }

    @Test
    @DisplayName("searchComplex — Native, пустой результат")
    void searchComplex_native_empty() {
        when(comicRepository.findByGenreAndYearNative(any(), any(), any()))
            .thenReturn(Collections.emptyList());

        List<ComicDto> result =
            comicService.searchComplex("Жанр", 1999, 0, 5, true);

        assertTrue(result.isEmpty());
    }

// ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — успех")
    void create_success() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L));
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(testComic);

        ComicDto result = comicService.create(request);

        assertNotNull(result);
        assertEquals("Берсерк", result.title());
        verify(comicRepository).save(any(Comic.class));
    }

    @Test
    @DisplayName("create — автор не найден — ValidationException")
    void create_authorNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 99L, 1L, Set.of(1L));
        when(authorRepository.existsById(99L)).thenReturn(false);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));

        ValidationException ex = assertThrows(ValidationException.class,
            () -> comicService.create(request));

        assertTrue(ex.getErrors().containsKey("authorId"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — издатель не найден — ValidationException")
    void create_publisherNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 99L, Set.of(1L));
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(99L)).thenReturn(false);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));

        ValidationException ex = assertThrows(ValidationException.class,
            () -> comicService.create(request));

        assertTrue(ex.getErrors().containsKey("publisherId"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — жанр не найден — ValidationException")
    void create_genreNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(99L));
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        when(genreRepository.findAllById(Set.of(99L))).thenReturn(Collections.emptyList());

        ValidationException ex = assertThrows(ValidationException.class,
            () -> comicService.create(request));

        assertTrue(ex.getErrors().containsKey("genreIds"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — пустое название — ValidationException")
    void create_blankTitle() {
        ComicRequest request = new ComicRequest(
            "", 1989, 1L, 1L, Set.of(1L));

        ValidationException ex = assertThrows(ValidationException.class,
            () -> comicService.create(request));

        assertTrue(ex.getErrors().containsKey("title"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — null год — ValidationException")
    void create_nullYear() {
        ComicRequest request = new ComicRequest(
            "Берсерк", null, 1L, 1L, Set.of(1L));

        ValidationException ex = assertThrows(ValidationException.class,
            () -> comicService.create(request));

        assertTrue(ex.getErrors().containsKey("releaseYear"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — null authorId — ValidationException")
    void create_nullAuthorId() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, null, 1L, Set.of(1L));

        ValidationException ex = assertThrows(ValidationException.class,
            () -> comicService.create(request));

        assertTrue(ex.getErrors().containsKey("authorId"));
        verify(comicRepository, never()).save(any());
    }

// ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — успех")
    void update_success() {
        ComicRequest request = new ComicRequest(
            "Берсерк 2", 1990, 1L, 1L, Set.of(1L));
        Comic updated = new Comic();
        updated.setId(1L);
        updated.setTitle("Берсерк 2");
        updated.setReleaseYear(1990);
        updated.setAuthor(testAuthor);
        updated.setPublisher(testPublisher);
        updated.setGenres(Set.of(testGenre));

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(updated);

        ComicDto result = comicService.update(1L, request);

        assertEquals("Берсерк 2", result.title());
        verify(comicRepository).save(any(Comic.class));
    }

    @Test
    @DisplayName("update — комикс не найден")
    void update_notFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L));
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> comicService.update(99L, request));
    }

    @Test
    @DisplayName("update — издатель не найден — ValidationException")
    void update_publisherNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 99L, Set.of(1L));
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(99L)).thenReturn(false);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));

        ValidationException ex = assertThrows(ValidationException.class,
            () -> comicService.update(1L, request));

        assertTrue(ex.getErrors().containsKey("publisherId"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("update — жанры не найдены — ValidationException")
    void update_genresNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(99L));
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        when(genreRepository.findAllById(Set.of(99L))).thenReturn(Collections.emptyList());

        ValidationException ex = assertThrows(ValidationException.class,
            () -> comicService.update(1L, request));

        assertTrue(ex.getErrors().containsKey("genreIds"));
        verify(comicRepository, never()).save(any());
    }

// ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — успех")
    void delete_success() {
        when(comicRepository.existsById(1L)).thenReturn(true);

        comicService.delete(1L);

        verify(comicRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete — не найден")
    void delete_notFound() {
        when(comicRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
            () -> comicService.delete(99L));

        verify(comicRepository, never()).deleteById(any());
    }

// ─── patch ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("patch — обновляет title")
    void patch_title() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк: Новая Эра", null, null, null, null);
        Comic patched = new Comic();
        patched.setId(1L);
        patched.setTitle("Берсерк: Новая Эра");
        patched.setReleaseYear(1989);
        patched.setAuthor(testAuthor);
        patched.setPublisher(testPublisher);
        patched.setGenres(Set.of(testGenre));

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(comicRepository.save(any(Comic.class))).thenReturn(patched);

        ComicDto result = comicService.patch(1L, request);

        assertEquals("Берсерк: Новая Эра", result.title());
    }

    @Test
    @DisplayName("patch — обновляет releaseYear")
    void patch_releaseYear() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", 1990, null, null, null);
        Comic patched = new Comic();
        patched.setId(1L);
        patched.setTitle("Берсерк");
        patched.setReleaseYear(1990);
        patched.setAuthor(testAuthor);
        patched.setPublisher(testPublisher);
        patched.setGenres(Set.of(testGenre));

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(comicRepository.save(any(Comic.class))).thenReturn(patched);

        ComicDto result = comicService.patch(1L, request);

        assertEquals(1990, result.releaseYear());
    }

    @Test
    @DisplayName("patch — обновляет автора")
    void patch_author() {
        Author newAuthor = new Author();
        newAuthor.setId(2L);
        newAuthor.setName("Другой Автор");
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", null, 2L, null, null);
        Comic patched = new Comic();
        patched.setId(1L);
        patched.setTitle("Берсерк");
        patched.setReleaseYear(1989);
        patched.setAuthor(newAuthor);
        patched.setPublisher(testPublisher);
        patched.setGenres(Set.of(testGenre));

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(authorRepository.existsById(2L)).thenReturn(true);
        when(authorRepository.getReferenceById(2L)).thenReturn(newAuthor);
        when(comicRepository.save(any(Comic.class))).thenReturn(patched);

        ComicDto result = comicService.patch(1L, request);

        assertEquals("Другой Автор", result.author().name());
    }

    @Test
    @DisplayName("patch — обновляет publisherId")
    void patch_publisherId() {
        Publisher newPublisher = new Publisher();
        newPublisher.setId(2L);
        newPublisher.setName("Kodansha");
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", null, null, 2L, null);
        Comic patched = new Comic();
        patched.setId(1L);
        patched.setTitle("Берсерк");
        patched.setReleaseYear(1989);
        patched.setAuthor(testAuthor);
        patched.setPublisher(newPublisher);
        patched.setGenres(Set.of(testGenre));

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(publisherRepository.existsById(2L)).thenReturn(true);
        when(publisherRepository.getReferenceById(2L)).thenReturn(newPublisher);
        when(comicRepository.save(any(Comic.class))).thenReturn(patched);

        ComicDto result = comicService.patch(1L, request);

        assertEquals("Kodansha", result.publisher().name());
    }

    @Test
    @DisplayName("patch — обновляет жанры")
    void patch_genres() {
        Genre newGenre = new Genre();
        newGenre.setId(2L);
        newGenre.setName("Экшн");
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", null, null, null, Set.of(2L));
        Comic patched = new Comic();
        patched.setId(1L);
        patched.setTitle("Берсерк");
        patched.setReleaseYear(1989);
        patched.setAuthor(testAuthor);
        patched.setPublisher(testPublisher);
        patched.setGenres(Set.of(newGenre));

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(genreRepository.findAllById(Set.of(2L))).thenReturn(List.of(newGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(patched);

        ComicDto result = comicService.patch(1L, request);

        assertTrue(result.genres().stream().anyMatch(g -> g.name().equals("Экшн")));
    }

    @Test
    @DisplayName("patch — комикс не найден")
    void patch_notFound() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", null, null, null, null);
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> comicService.patch(99L, request));
    }

    @Test
    @DisplayName("patch — автор не найден — ValidationException")
    void patch_authorNotFound() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", null, 99L, null, null);
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(authorRepository.existsById(99L)).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class,
            () -> comicService.patch(1L, request));

        assertTrue(ex.getErrors().containsKey("authorId"));
    }

    @Test
    @DisplayName("patch — издатель не найден — ValidationException")
    void patch_publisherNotFound() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", null, null, 99L, null);
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(publisherRepository.existsById(99L)).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class,
            () -> comicService.patch(1L, request));

        assertTrue(ex.getErrors().containsKey("publisherId"));
    }

    @Test
    @DisplayName("patch — жанры не найдены — ValidationException")
    void patch_genresNotFound() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", null, null, null, Set.of(99L));
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(genreRepository.findAllById(Set.of(99L))).thenReturn(Collections.emptyList());

        ValidationException ex = assertThrows(ValidationException.class,
            () -> comicService.patch(1L, request));

        assertTrue(ex.getErrors().containsKey("genreIds"));
    }

    @Test
    @DisplayName("extractAuthorId — неизвестный тип — возвращает null")
    void extractAuthorId_unknownType_returnsNull() throws Exception {
        var method = ComicService.class
            .getDeclaredMethod("extractAuthorId", Object.class);
        method.setAccessible(true);

        Object result = method.invoke(comicService, "some_unknown_object");

        assertNull(result);
    }

    @Test
    @DisplayName("extractPublisherId — неизвестный тип — возвращает null")
    void extractPublisherId_unknownType_returnsNull() throws Exception {
        var method = ComicService.class
            .getDeclaredMethod("extractPublisherId", Object.class);
        method.setAccessible(true);

        Object result = method.invoke(comicService, "some_unknown_object");

        assertNull(result);
    }

    @Test
    @DisplayName("extractGenreIds — неизвестный тип — возвращает emptySet")
    void extractGenreIds_unknownType_returnsEmptySet() throws Exception {
        var method = ComicService.class
            .getDeclaredMethod("extractGenreIds", Object.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Set<Long> result = (Set<Long>) method.invoke(comicService, "some_unknown_object");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("searchComplex — Native, genreIds не null, genreNames null")
    void searchComplex_native_genreIdsNotNull_genreNamesNull() {
        ComicNativeProjection proj = new ComicNativeProjection() {
            @Override public Long getId() { return 1L; }
            @Override public String getTitle() { return "Берсерк"; }
            @Override public Integer getReleaseYear() { return 1989; }
            @Override public Long getAuthorId() { return 1L; }
            @Override public String getAuthorName() { return "Кэнтаро Миура"; }
            @Override public Long getPublisherId() { return 1L; }
            @Override public String getPublisherName() { return "Hakusensha"; }
            @Override public String getGenreIds() { return "1"; }
            @Override public String getGenreNames() { return null; } // null!
        };

        when(comicRepository.findByGenreAndYearNative(any(), any(), any()))
            .thenReturn(List.of(proj));

        List<ComicDto> result =
            comicService.searchComplex("Жанр", 1989, 0, 5, true);

        assertEquals(1, result.size());
        assertTrue(result.get(0).genres().isEmpty());
    }

    @Test
    @DisplayName("patch — только title, остальные поля null")
    void patch_allNull() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", null, null, null, null);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(comicRepository.save(any(Comic.class))).thenReturn(testComic);

        ComicDto result = comicService.patch(1L, request);

        assertEquals("Берсерк", result.title());
        assertEquals(1989, result.releaseYear());
        verify(comicRepository).save(any(Comic.class));
    }

    @Test
    @DisplayName("create — пустой genreIds — ValidationException от аннотации")
    void create_emptyGenreIds() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Collections.emptySet());

        assertThrows(ValidationException.class,
            () -> comicService.create(request));

        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("update — часть жанров не найдена — ValidationException")
    void update_partialGenresNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L, 2L));

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        // возвращаем только один жанр из двух запрошенных
        when(genreRepository.findAllById(Set.of(1L, 2L)))
            .thenReturn(List.of(testGenre)); // найден только ID=1, ID=2 отсутствует

        ValidationException ex = assertThrows(ValidationException.class,
            () -> comicService.update(1L, request));

        assertTrue(ex.getErrors().containsKey("genreIds"));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("patch — title null — ValidationException")
    void patch_titleNull() {
        ComicPatchRequest request = new ComicPatchRequest(
            null, 1990, null, null, null);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));

        assertThrows(ValidationException.class,
            () -> comicService.patch(1L, request));

        verify(comicRepository, never()).save(any());
    }
}