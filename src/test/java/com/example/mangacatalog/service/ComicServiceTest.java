package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.*;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Genre;
import com.example.mangacatalog.entity.Publisher;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.exception.ValidationException;
import com.example.mangacatalog.mapper.ComicMapper;
import com.example.mangacatalog.repository.AuthorRepository;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.GenreRepository;
import com.example.mangacatalog.repository.PublisherRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    @Mock
    private ComicMapper comicMapper;
    @Mock
    private ApiCacheManager cacheManager;
    @Mock
    private Validator validator;

    @InjectMocks
    private ComicService comicService;

    private Author testAuthor;
    private Publisher testPublisher;
    private Genre testGenre;
    private Comic testComic;
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

        testComic = new Comic();
        testComic.setId(1L);
        testComic.setTitle("Берсерк");
        testComic.setReleaseYear(1989);
        testComic.setAuthor(testAuthor);
        testComic.setPublisher(testPublisher);
        testComic.setGenres(Set.of(testGenre));

        testComicDto = new ComicDto(
            1L, "Берсерк", 1989,
            new AuthorDto(1L, "Кэнтаро Миура"),
            new PublisherDto(1L, "Hakusensha"),
            Set.of(new GenreDto(1L, "Тёмное фэнтези"))
        );
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Настраивает validator.validate() чтобы возвращал пустой Set (нет ошибок аннотаций).
     * Вызывать перед операциями create/update/patch/delete.
     */
    @SuppressWarnings("unchecked")
    private void noValidationErrors() {
        when(validator.validate(any())).thenReturn(Collections.emptySet());
    }

    /**
     * Настраивает мок-нарушение валидации с заданным полем и сообщением.
     */
    @SuppressWarnings("unchecked")
    private void withValidationError(String field, String message) {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn(field);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);
        when(validator.validate(any())).thenReturn(Set.of(violation));
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll — из кэша")
    void getAll_fromCache() {
        List<ComicDto> cached = List.of(testComicDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<ComicDto> result = comicService.getAll();

        assertEquals(cached, result);
        verify(comicRepository, never()).findAll();
    }

    @Test
    @DisplayName("getAll — кэш пуст, запрос к БД")
    void getAll_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findAll()).thenReturn(List.of(testComic));
        when(comicMapper.toDto(testComic)).thenReturn(testComicDto);

        List<ComicDto> result = comicService.getAll();

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getAll — пустой список")
    void getAll_empty() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(comicService.getAll().isEmpty());
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — из кэша")
    void getById_fromCache() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(testComicDto);

        ComicDto result = comicService.getById(1L);

        assertEquals(testComicDto, result);
        verify(comicRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getById — кэш пуст, успех")
    void getById_cacheMiss_success() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(comicMapper.toDto(testComic)).thenReturn(testComicDto);

        ComicDto result = comicService.getById(1L);

        assertNotNull(result);
        assertEquals("Берсерк", result.title());
        assertEquals(1989, result.releaseYear());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getById — комикс не найден")
    void getById_notFound() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> comicService.getById(99L));
    }

    // ─── searchByTitle ────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchByTitle — из кэша")
    void searchByTitle_fromCache() {
        List<ComicDto> cached = List.of(testComicDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<ComicDto> result = comicService.searchByTitle("Берс");

        assertEquals(cached, result);
        verify(comicRepository, never()).findByTitleContainingIgnoreCase(any());
    }

    @Test
    @DisplayName("searchByTitle — кэш пуст, нашёл результаты")
    void searchByTitle_cacheMiss_found() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByTitleContainingIgnoreCase("Берс"))
            .thenReturn(List.of(testComic));
        when(comicMapper.toDto(testComic)).thenReturn(testComicDto);

        List<ComicDto> result = comicService.searchByTitle("Берс");

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("searchByTitle — ничего не найдено")
    void searchByTitle_empty() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByTitleContainingIgnoreCase("xyz"))
            .thenReturn(Collections.emptyList());

        assertTrue(comicService.searchByTitle("xyz").isEmpty());
    }

    // ─── getComicsByAuthor ────────────────────────────────────────────────────

    @Test
    @DisplayName("getComicsByAuthor — из кэша")
    void getComicsByAuthor_fromCache() {
        List<ComicDto> cached = List.of(testComicDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<ComicDto> result = comicService.getComicsByAuthor(1L);

        assertEquals(cached, result);
        verify(comicRepository, never()).findByAuthorId(any());
    }

    @Test
    @DisplayName("getComicsByAuthor — кэш пуст, запрос к БД")
    void getComicsByAuthor_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByAuthorId(1L)).thenReturn(List.of(testComic));
        when(comicMapper.toDto(testComic)).thenReturn(testComicDto);

        List<ComicDto> result = comicService.getComicsByAuthor(1L);

        assertEquals(1, result.size());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    // ─── searchComplex (JPQL) ─────────────────────────────────────────────────

    @Test
    @DisplayName("searchComplex — из кэша")
    void searchComplex_fromCache() {
        List<ComicDto> cached = List.of(testComicDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<ComicDto> result = comicService.searchComplex("Сёнэн", 2000, 0, 5, false);

        assertEquals(cached, result);
        verify(comicRepository, never()).findByGenreAndYearJpql(any(), any(), any());
    }

    @Test
    @DisplayName("searchComplex — JPQL, кэш пуст")
    void searchComplex_jpql_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearJpql(
            eq("Сёнэн"), eq(2000), any(Pageable.class)))
            .thenReturn(List.of(testComic));
        when(comicMapper.toDto(testComic)).thenReturn(testComicDto);

        List<ComicDto> result = comicService.searchComplex("Сёнэн", 2000, 0, 5, false);

        assertEquals(1, result.size());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("searchComplex — Native, кэш пуст")
    void searchComplex_native_cacheMiss() {
        ComicNativeProjection proj = mock(ComicNativeProjection.class);
        when(proj.getId()).thenReturn(1L);
        when(proj.getTitle()).thenReturn("Берсерк");
        when(proj.getReleaseYear()).thenReturn(1989);
        when(proj.getAuthorId()).thenReturn(1L);
        when(proj.getAuthorName()).thenReturn("Кэнтаро Миура");
        when(proj.getPublisherId()).thenReturn(1L);
        when(proj.getPublisherName()).thenReturn("Hakusensha");
        when(proj.getGenreIds()).thenReturn("1");
        when(proj.getGenreNames()).thenReturn("Тёмное фэнтези");

        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearNative(
            eq("Тёмное фэнтези"), eq(1989), any(Pageable.class)))
            .thenReturn(List.of(proj));

        List<ComicDto> result = comicService.searchComplex("Тёмное фэнтези", 1989, 0, 5, true);

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("searchComplex — Native, null поля автора и издателя")
    void searchComplex_native_nullAuthorAndPublisher() {
        ComicNativeProjection proj = mock(ComicNativeProjection.class);
        when(proj.getId()).thenReturn(2L);
        when(proj.getTitle()).thenReturn("Без автора");
        when(proj.getReleaseYear()).thenReturn(2000);
        when(proj.getAuthorId()).thenReturn(null);
        when(proj.getPublisherId()).thenReturn(null);
        when(proj.getGenreIds()).thenReturn(null);
        when(proj.getGenreNames()).thenReturn(null);

        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearNative(any(), any(), any()))
            .thenReturn(List.of(proj));

        List<ComicDto> result = comicService.searchComplex("Жанр", 1999, 0, 5, true);

        assertEquals(1, result.size());
        assertNull(result.get(0).author());
        assertNull(result.get(0).publisher());
        assertTrue(result.get(0).genres().isEmpty());
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — успех")
    void create_success() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L));

        noValidationErrors();
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(testComic);
        when(comicMapper.toDto(testComic)).thenReturn(testComicDto);

        ComicDto result = comicService.create(request);

        assertNotNull(result);
        assertEquals("Берсерк", result.title());
        verify(comicRepository).save(any(Comic.class));
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("create — автор не найден, бросает ValidationException")
    void create_authorNotFound_throwsValidation() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 99L, 1L, Set.of(1L));

        noValidationErrors();
        when(authorRepository.existsById(99L)).thenReturn(false);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));

        assertThrows(ValidationException.class,
            () -> comicService.create(request));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — издатель не найден, бросает ValidationException")
    void create_publisherNotFound_throwsValidation() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 99L, Set.of(1L));

        noValidationErrors();
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(99L)).thenReturn(false);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));

        assertThrows(ValidationException.class,
            () -> comicService.create(request));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — жанры не найдены, бросает ValidationException")
    void create_genresNotFound_throwsValidation() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(99L));

        noValidationErrors();
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        // Жанр не найден — возвращаем пустой список
        when(genreRepository.findAllById(Set.of(99L))).thenReturn(Collections.emptyList());

        assertThrows(ValidationException.class,
            () -> comicService.create(request));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — нарушение аннотационной валидации, бросает ValidationException")
    void create_annotationValidationFails() {
        ComicRequest request = new ComicRequest(
            "", 1989, 1L, 1L, Set.of(1L));

        withValidationError("title", "Название комикса не может быть пустым");

        assertThrows(ValidationException.class,
            () -> comicService.create(request));
        verify(comicRepository, never()).save(any());
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — успех")
    void update_success() {
        ComicRequest request = new ComicRequest(
            "Берсерк обновлён", 1990, 1L, 1L, Set.of(1L));

        noValidationErrors();
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(testComic);
        when(comicMapper.toDto(testComic)).thenReturn(testComicDto);

        ComicDto result = comicService.update(1L, request);

        assertNotNull(result);
        verify(comicRepository).save(any(Comic.class));
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("update — комикс не найден")
    void update_comicNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L));
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> comicService.update(99L, request));
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — успех")
    void delete_success() {
        when(comicRepository.existsById(1L)).thenReturn(true);

        comicService.delete(1L);

        verify(comicRepository).deleteById(1L);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("delete — комикс не найден")
    void delete_notFound() {
        when(comicRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
            () -> comicService.delete(99L));
        verify(comicRepository, never()).deleteById(any());
    }

    // ─── patch ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("patch — обновляет только title")
    void patch_titleOnly() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк: Новая Эра", null, null, null, null);
        ComicDto patchedDto = new ComicDto(
            1L, "Берсерк: Новая Эра", 1989,
            new AuthorDto(1L, "Кэнтаро Миура"),
            new PublisherDto(1L, "Hakusensha"),
            Set.of(new GenreDto(1L, "Тёмное фэнтези"))
        );

        noValidationErrors();
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(comicRepository.save(any(Comic.class))).thenReturn(testComic);
        when(comicMapper.toDto(testComic)).thenReturn(patchedDto);

        ComicDto result = comicService.patch(1L, request);

        assertEquals("Берсерк: Новая Эра", result.title());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("patch — обновляет authorId")
    void patch_authorId() {
        Author newAuthor = new Author();
        newAuthor.setId(2L);
        newAuthor.setName("Другой Автор");

        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", null, 2L, null, null);

        noValidationErrors();
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(authorRepository.existsById(2L)).thenReturn(true);
        when(authorRepository.getReferenceById(2L)).thenReturn(newAuthor);
        when(comicRepository.save(any(Comic.class))).thenReturn(testComic);
        when(comicMapper.toDto(testComic)).thenReturn(testComicDto);

        comicService.patch(1L, request);

        verify(authorRepository).getReferenceById(2L);
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("patch — обновляет genreIds")
    void patch_genreIds() {
        Genre newGenre = new Genre();
        newGenre.setId(2L);
        newGenre.setName("Экшн");

        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", null, null, null, Set.of(2L));

        noValidationErrors();
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(genreRepository.findAllById(Set.of(2L))).thenReturn(List.of(newGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(testComic);
        when(comicMapper.toDto(testComic)).thenReturn(testComicDto);

        comicService.patch(1L, request);

        verify(genreRepository).findAllById(Set.of(2L));
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("patch — комикс не найден")
    void patch_comicNotFound() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", null, null, null, null);
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> comicService.patch(99L, request));
    }

    @Test
    @DisplayName("patch — автор не найден, бросает ValidationException")
    void patch_authorNotFound_throwsValidation() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк", null, 99L, null, null);

        noValidationErrors();
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(authorRepository.existsById(99L)).thenReturn(false);

        assertThrows(ValidationException.class,
            () -> comicService.patch(1L, request));
        verify(comicRepository, never()).save(any());
    }
}