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
import com.example.mangacatalog.mapper.AuthorMapper;
import com.example.mangacatalog.mapper.ComicMapper;
import com.example.mangacatalog.mapper.GenreMapper;
import com.example.mangacatalog.mapper.PublisherMapper;
import com.example.mangacatalog.repository.AuthorRepository;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.GenreRepository;
import com.example.mangacatalog.repository.PublisherRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
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
    private ApiCacheManager cacheManager;

    @Mock
    private Validator validator;

    @Spy
    private ComicMapper comicMapper = new ComicMapper(
        new AuthorMapper(),
        new PublisherMapper(),
        new GenreMapper()
    );

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
        testPublisher.setName("Shueisha");

        testGenre = new Genre();
        testGenre.setId(1L);
        testGenre.setName("Сёнэн");

        testComic = new Comic();
        testComic.setId(1L);
        testComic.setTitle("Берсерк");
        testComic.setReleaseYear(1989);
        testComic.setAuthor(testAuthor);
        testComic.setPublisher(testPublisher);
        testComic.getGenres().add(testGenre);

        testComicDto = new ComicDto(
            1L,
            "Берсерк",
            1989,
            new AuthorDto(1L, "Кэнтаро Миура"),
            new PublisherDto(1L, "Shueisha"),
            Set.of(new GenreDto(1L, "Сёнэн"))
        );

        lenient().when(validator.validate(any()))
            .thenReturn(Collections.emptySet());
    }


    @Test
    @DisplayName("getAll — кеш попадание, репозиторий не вызывается")
    void getAll_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class)))
            .thenReturn(List.of(testComicDto));

        List<ComicDto> result = comicService.getAll();

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
        verify(comicRepository, never()).findAll();
        verify(cacheManager, never()).put(any(), any());
    }

    @Test
    @DisplayName("getAll — кеш промах, данные из БД кешируются")
    void getAll_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findAll()).thenReturn(List.of(testComic));

        List<ComicDto> result = comicService.getAll();

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
        verify(comicRepository).findAll();
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getAll — пустой список")
    void getAll_empty() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findAll()).thenReturn(Collections.emptyList());

        List<ComicDto> result = comicService.getAll();

        assertTrue(result.isEmpty());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }


    @Test
    @DisplayName("getById — кеш попадание")
    void getById_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(testComicDto);

        ComicDto result = comicService.getById(1L);

        assertNotNull(result);
        assertEquals("Берсерк", result.title());
        verify(comicRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getById — кеш промах, успех")
    void getById_cacheMiss_success() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));

        ComicDto result = comicService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Берсерк", result.title());
        assertEquals(1989, result.releaseYear());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getById — не найден, выброс ResourceNotFoundException")
    void getById_notFound() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> comicService.getById(99L));
        verify(cacheManager, never()).put(any(), any());
    }


    @Test
    @DisplayName("searchByTitle — кеш попадание")
    void searchByTitle_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class)))
            .thenReturn(List.of(testComicDto));

        List<ComicDto> result = comicService.searchByTitle("Берсерк");

        assertEquals(1, result.size());
        verify(comicRepository, never()).findByTitleContainingIgnoreCase(any());
    }

    @Test
    @DisplayName("searchByTitle — кеш промах, поиск по названию")
    void searchByTitle_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByTitleContainingIgnoreCase("Берсерк"))
            .thenReturn(List.of(testComic));

        List<ComicDto> result = comicService.searchByTitle("Берсерк");

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("searchByTitle — ничего не найдено")
    void searchByTitle_noResults() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByTitleContainingIgnoreCase("xyz"))
            .thenReturn(Collections.emptyList());

        List<ComicDto> result = comicService.searchByTitle("xyz");

        assertTrue(result.isEmpty());
    }



    @Test
    @DisplayName("getComicsByAuthor — кеш попадание")
    void getComicsByAuthor_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class)))
            .thenReturn(List.of(testComicDto));

        List<ComicDto> result = comicService.getComicsByAuthor(1L);

        assertEquals(1, result.size());
        verify(comicRepository, never()).findByAuthorId(any());
    }

    @Test
    @DisplayName("getComicsByAuthor — кеш промах")
    void getComicsByAuthor_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByAuthorId(1L)).thenReturn(List.of(testComic));

        List<ComicDto> result = comicService.getComicsByAuthor(1L);

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("getComicsByAuthor — автор без комиксов")
    void getComicsByAuthor_empty() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByAuthorId(1L)).thenReturn(Collections.emptyList());

        List<ComicDto> result = comicService.getComicsByAuthor(1L);

        assertTrue(result.isEmpty());
    }



    @Test
    @DisplayName("searchComplex JPQL — кеш попадание")
    void searchComplex_jpql_cacheHit() {
        when(cacheManager.get(any(ApiCacheKey.class)))
            .thenReturn(List.of(testComicDto));

        List<ComicDto> result = comicService.searchComplex(
            "Сёнэн", 1980, 0, 10, false);

        assertEquals(1, result.size());
        verify(comicRepository, never()).findByGenreAndYearJpql(
            any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchComplex JPQL — кеш промах, данные из БД")
    void searchComplex_jpql_cacheMiss() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearJpql(
            eq("Сёнэн"), eq(1980), any(Pageable.class)))
            .thenReturn(List.of(testComic));

        List<ComicDto> result = comicService.searchComplex(
            "Сёнэн", 1980, 0, 10, false);

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("searchComplex JPQL — пустой результат")
    void searchComplex_jpql_empty() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearJpql(
            any(), any(), any(Pageable.class)))
            .thenReturn(Collections.emptyList());

        List<ComicDto> result = comicService.searchComplex(
            "Сёнэн", 1980, 0, 10, false);

        assertTrue(result.isEmpty());
    }



    @Test
    @DisplayName("searchComplex Native — все поля заполнены")
    void searchComplex_native_allFields() {
        ComicNativeProjection projection = mock(ComicNativeProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getTitle()).thenReturn("Берсерк");
        when(projection.getReleaseYear()).thenReturn(1989);
        when(projection.getAuthorId()).thenReturn(1L);
        when(projection.getAuthorName()).thenReturn("Кэнтаро Миура");
        when(projection.getPublisherId()).thenReturn(1L);
        when(projection.getPublisherName()).thenReturn("Shueisha");
        when(projection.getGenreIds()).thenReturn("1");
        when(projection.getGenreNames()).thenReturn("Сёнэн");

        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearNative(
            eq("Сёнэн"), eq(1980), any(Pageable.class)))
            .thenReturn(List.of(projection));

        List<ComicDto> result = comicService.searchComplex(
            "Сёнэн", 1980, 0, 10, true);

        assertEquals(1, result.size());
        assertEquals("Берсерк", result.get(0).title());
        assertEquals(1989, result.get(0).releaseYear());
        assertNotNull(result.get(0).author());
        assertEquals("Кэнтаро Миура", result.get(0).author().name());
        assertNotNull(result.get(0).publisher());
        assertEquals("Shueisha", result.get(0).publisher().name());
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    @DisplayName("searchComplex Native — authorId и publisherId null")
    void searchComplex_native_nullAuthorAndPublisher() {
        ComicNativeProjection projection = mock(ComicNativeProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getTitle()).thenReturn("Берсерк");
        when(projection.getReleaseYear()).thenReturn(1989);
        when(projection.getAuthorId()).thenReturn(null);
        when(projection.getPublisherId()).thenReturn(null);
        when(projection.getGenreIds()).thenReturn(null);
        when(projection.getGenreNames()).thenReturn(null);

        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearNative(
            any(), any(), any(Pageable.class)))
            .thenReturn(List.of(projection));

        List<ComicDto> result = comicService.searchComplex(
            "Сёнэн", 1980, 0, 10, true);

        assertEquals(1, result.size());
        assertNull(result.get(0).author());
        assertNull(result.get(0).publisher());
        assertTrue(result.get(0).genres().isEmpty());
    }

    @Test
    @DisplayName("searchComplex Native — несколько жанров через запятую")
    void searchComplex_native_multipleGenres() {
        ComicNativeProjection projection = mock(ComicNativeProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getTitle()).thenReturn("Берсерк");
        when(projection.getReleaseYear()).thenReturn(1989);
        when(projection.getAuthorId()).thenReturn(null);
        when(projection.getPublisherId()).thenReturn(null);
        when(projection.getGenreIds()).thenReturn("1,2");
        when(projection.getGenreNames()).thenReturn("Сёнэн,Экшен");

        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearNative(
            any(), any(), any(Pageable.class)))
            .thenReturn(List.of(projection));

        List<ComicDto> result = comicService.searchComplex(
            "Сёнэн", 1980, 0, 10, true);

        assertEquals(2, result.get(0).genres().size());
    }



    @Test
    @DisplayName("create — успешное создание")
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
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("create — автор не найден, выброс ValidationException")
    void create_authorNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 99L, 1L, Set.of(1L));

        when(authorRepository.existsById(99L)).thenReturn(false);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));

        assertThrows(ValidationException.class,
            () -> comicService.create(request));
        verify(comicRepository, never()).save(any());
        verify(cacheManager, never()).invalidate();
    }

    @Test
    @DisplayName("create — издатель не найден, выброс ValidationException")
    void create_publisherNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 99L, Set.of(1L));

        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(99L)).thenReturn(false);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));

        assertThrows(ValidationException.class,
            () -> comicService.create(request));
        verify(comicRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — жанры не найдены, выброс ValidationException")
    void create_genresNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк", 1989, 1L, 1L, Set.of(1L, 999L));

        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        when(genreRepository.findAllById(Set.of(1L, 999L)))
            .thenReturn(List.of(testGenre));

        assertThrows(ValidationException.class,
            () -> comicService.create(request));
        verify(comicRepository, never()).save(any());
    }


    @Test
    @DisplayName("update — успешное обновление")
    void update_success() {
        ComicRequest request = new ComicRequest(
            "Берсерк 2", 1990, 1L, 1L, Set.of(1L));

        Comic updatedComic = new Comic();
        updatedComic.setId(1L);
        updatedComic.setTitle("Берсерк 2");
        updatedComic.setReleaseYear(1990);
        updatedComic.setAuthor(testAuthor);
        updatedComic.setPublisher(testPublisher);
        updatedComic.getGenres().add(testGenre);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(updatedComic);

        ComicDto result = comicService.update(1L, request);

        assertNotNull(result);
        assertEquals("Берсерк 2", result.title());
        assertEquals(1990, result.releaseYear());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("update — комикс не найден")
    void update_comicNotFound() {
        ComicRequest request = new ComicRequest(
            "Берсерк 2", 1990, 1L, 1L, Set.of(1L));
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> comicService.update(99L, request));
        verify(comicRepository, never()).save(any());
        verify(cacheManager, never()).invalidate();
    }



    @Test
    @DisplayName("delete — успешное удаление")
    void delete_success() {
        when(comicRepository.existsById(1L)).thenReturn(true);
        doNothing().when(comicRepository).deleteById(1L);

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
        verify(cacheManager, never()).invalidate();
    }


    @Test
    @DisplayName("patch — обновление только названия")
    void patch_onlyTitle() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Новое название", null, null, null, null);

        Comic updated = new Comic();
        updated.setId(1L);
        updated.setTitle("Новое название");
        updated.setReleaseYear(1989);
        updated.setAuthor(testAuthor);
        updated.setPublisher(testPublisher);
        updated.getGenres().add(testGenre);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(comicRepository.save(any(Comic.class))).thenReturn(updated);

        ComicDto result = comicService.patch(1L, request);

        assertNotNull(result);
        assertEquals("Новое название", result.title());
        verify(authorRepository, never()).existsById(any());
        verify(publisherRepository, never()).existsById(any());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("patch — обновление всех полей")
    void patch_allFields() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Берсерк 2", 1990, 1L, 1L, Set.of(1L));

        Comic updated = new Comic();
        updated.setId(1L);
        updated.setTitle("Берсерк 2");
        updated.setReleaseYear(1990);
        updated.setAuthor(testAuthor);
        updated.setPublisher(testPublisher);
        updated.getGenres().add(testGenre);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(testAuthor);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(testPublisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(testGenre));
        when(comicRepository.save(any(Comic.class))).thenReturn(updated);

        ComicDto result = comicService.patch(1L, request);

        assertNotNull(result);
        assertEquals("Берсерк 2", result.title());
        assertEquals(1990, result.releaseYear());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("patch — все поля null, изменений нет")
    void patch_noFieldsChanged() {
        ComicPatchRequest request = new ComicPatchRequest(
            null, null, null, null, null);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(comicRepository.save(any(Comic.class))).thenReturn(testComic);

        ComicDto result = comicService.patch(1L, request);

        assertNotNull(result);
        verify(authorRepository, never()).existsById(any());
        verify(publisherRepository, never()).existsById(any());
        verify(genreRepository, never()).findAllById(any());
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("patch — комикс не найден")
    void patch_comicNotFound() {
        ComicPatchRequest request = new ComicPatchRequest(
            "Название", null, null, null, null);
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> comicService.patch(99L, request));
        verify(comicRepository, never()).save(any());
        verify(cacheManager, never()).invalidate();
    }

    @Test
    @DisplayName("patch — автор не найден при обновлении")
    void patch_authorNotFound() {
        ComicPatchRequest request = new ComicPatchRequest(
            null, null, 99L, null, null);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(authorRepository.existsById(99L)).thenReturn(false);

        assertThrows(ValidationException.class,
            () -> comicService.patch(1L, request));
        verify(comicRepository, never()).save(any());
        verify(cacheManager, never()).invalidate();
    }

    @Test
    @DisplayName("patch — издатель не найден при обновлении")
    void patch_publisherNotFound() {
        ComicPatchRequest request = new ComicPatchRequest(
            null, null, null, 99L, null);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(testComic));
        when(publisherRepository.existsById(99L)).thenReturn(false);

        assertThrows(ValidationException.class,
            () -> comicService.patch(1L, request));
        verify(comicRepository, never()).save(any());
    }
}