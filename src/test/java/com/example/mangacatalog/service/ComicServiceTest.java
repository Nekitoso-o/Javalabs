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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private Comic comic;
    private ComicDto comicDto;
    private Author author;
    private Publisher publisher;
    private Genre genre;

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setId(1L);
        author.setName("Author");

        publisher = new Publisher();
        publisher.setId(1L);
        publisher.setName("Publisher");

        genre = new Genre();
        genre.setId(1L);
        genre.setName("Action");

        comic = new Comic();
        comic.setId(1L);
        comic.setTitle("Test Comic");
        comic.setReleaseYear(2000);
        comic.setAuthor(author);
        comic.setPublisher(publisher);
        comic.setGenres(Set.of(genre));

        comicDto = new ComicDto(1L, "Test Comic", 2000,
            new AuthorDto(1L, "Author"),
            new PublisherDto(1L, "Publisher"),
            Set.of(new GenreDto(1L, "Action")));
    }

    // ========== getAll() ==========

    @Test
    void getAll_whenCacheHit_returnsCached() {
        List<ComicDto> cached = List.of(comicDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<ComicDto> result = comicService.getAll();

        assertThat(result).isEqualTo(cached);
        verify(comicRepository, never()).findAll();
    }

    @Test
    void getAll_whenCacheMiss_fetchesAndCaches() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findAll()).thenReturn(List.of(comic));
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        List<ComicDto> result = comicService.getAll();

        assertThat(result).containsExactly(comicDto);
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    // ========== getById() ==========

    @Test
    void getById_whenCacheHit_returnsCached() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(comicDto);

        ComicDto result = comicService.getById(1L);

        assertThat(result).isEqualTo(comicDto);
        verify(comicRepository, never()).findById(any());
    }

    @Test
    void getById_whenCacheMiss_fetchesAndCaches() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findById(1L)).thenReturn(Optional.of(comic));
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        ComicDto result = comicService.getById(1L);

        assertThat(result).isEqualTo(comicDto);
        verify(cacheManager).put(any(ApiCacheKey.class), eq(comicDto));
    }

    @Test
    void getById_whenNotFound_throwsException() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicService.getById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ========== searchByTitle() ==========

    @Test
    void searchByTitle_whenCacheHit_returnsCached() {
        List<ComicDto> cached = List.of(comicDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<ComicDto> result = comicService.searchByTitle("Test");

        assertThat(result).isEqualTo(cached);
    }

    @Test
    void searchByTitle_whenCacheMiss_fetchesAndCaches() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByTitleContainingIgnoreCase("Test"))
            .thenReturn(List.of(comic));
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        List<ComicDto> result = comicService.searchByTitle("Test");

        assertThat(result).containsExactly(comicDto);
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    // ========== getComicsByAuthor() ==========

    @Test
    void getComicsByAuthor_whenCacheHit_returnsCached() {
        List<ComicDto> cached = List.of(comicDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<ComicDto> result = comicService.getComicsByAuthor(1L);

        assertThat(result).isEqualTo(cached);
    }

    @Test
    void getComicsByAuthor_whenCacheMiss_fetchesAndCaches() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByAuthorId(1L)).thenReturn(List.of(comic));
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        List<ComicDto> result = comicService.getComicsByAuthor(1L);

        assertThat(result).containsExactly(comicDto);
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    // ========== searchComplex() ==========

    @Test
    void searchComplex_whenCacheHit_returnsCached() {
        List<ComicDto> cached = List.of(comicDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<ComicDto> result = comicService.searchComplex("Action", 2000, 0, 5, false);

        assertThat(result).isEqualTo(cached);
    }

    @Test
    void searchComplex_whenCacheMiss_useJpql() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearJpql(
            eq("Action"), eq(2000), any(Pageable.class)))
            .thenReturn(List.of(comic));
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        List<ComicDto> result = comicService.searchComplex("Action", 2000, 0, 5, false);

        assertThat(result).containsExactly(comicDto);
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    void searchComplex_whenCacheMiss_useNative_withSingleGenre() {
        ComicNativeProjection proj = mock(ComicNativeProjection.class);
        when(proj.getId()).thenReturn(1L);
        when(proj.getTitle()).thenReturn("Test Comic");
        when(proj.getReleaseYear()).thenReturn(2000);
        when(proj.getAuthorId()).thenReturn(1L);
        when(proj.getAuthorName()).thenReturn("Author");
        when(proj.getPublisherId()).thenReturn(1L);
        when(proj.getPublisherName()).thenReturn("Publisher");
        when(proj.getGenreIds()).thenReturn("1");
        when(proj.getGenreNames()).thenReturn("Action");

        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearNative(
            eq("Action"), eq(2000), any(Pageable.class)))
            .thenReturn(List.of(proj));

        List<ComicDto> result = comicService.searchComplex("Action", 2000, 0, 5, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Test Comic");
        assertThat(result.get(0).genres()).hasSize(1);
        assertThat(result.get(0).author()).isNotNull();
        assertThat(result.get(0).publisher()).isNotNull();
    }

    @Test
    void searchComplex_useNative_withMultipleGenres_coversLoop() {
        // Покрывает цикл for (int i = 0; i < ids.length; i++) с несколькими жанрами
        ComicNativeProjection proj = mock(ComicNativeProjection.class);
        when(proj.getId()).thenReturn(3L);
        when(proj.getTitle()).thenReturn("Multi Genre Comic");
        when(proj.getReleaseYear()).thenReturn(2005);
        when(proj.getAuthorId()).thenReturn(1L);
        when(proj.getAuthorName()).thenReturn("Author");
        when(proj.getPublisherId()).thenReturn(1L);
        when(proj.getPublisherName()).thenReturn("Publisher");
        when(proj.getGenreIds()).thenReturn("1,2");
        when(proj.getGenreNames()).thenReturn("Action, Horror");

        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearNative(any(), any(), any(Pageable.class)))
            .thenReturn(List.of(proj));

        List<ComicDto> result = comicService.searchComplex("Action", 2000, 0, 5, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).genres()).hasSize(2);
    }

    @Test
    void searchComplex_nativeProjection_withNullAuthorAndPublisher() {
        // Покрывает ветки: authorId == null → author = null, publisherId == null → publisher = null
        ComicNativeProjection proj = mock(ComicNativeProjection.class);
        when(proj.getId()).thenReturn(2L);
        when(proj.getTitle()).thenReturn("No Author Comic");
        when(proj.getReleaseYear()).thenReturn(1990);
        when(proj.getAuthorId()).thenReturn(null);
        when(proj.getPublisherId()).thenReturn(null);
        when(proj.getGenreIds()).thenReturn(null);
        // getGenreNames() НЕ stubируем — не вызывается из-за short-circuit &&

        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearNative(any(), any(), any(Pageable.class)))
            .thenReturn(List.of(proj));

        List<ComicDto> result = comicService.searchComplex(null, null, 0, 5, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).author()).isNull();
        assertThat(result.get(0).publisher()).isNull();
        assertThat(result.get(0).genres()).isEmpty();
    }

    @Test
    void searchComplex_nativeProjection_genreIdsNotNull_butGenreNamesNull() {
        // Покрывает ветку: genreIds != null && genreNames == null → блок if не выполняется
        ComicNativeProjection proj = mock(ComicNativeProjection.class);
        when(proj.getId()).thenReturn(4L);
        when(proj.getTitle()).thenReturn("Comic");
        when(proj.getReleaseYear()).thenReturn(2000);
        when(proj.getAuthorId()).thenReturn(null);
        when(proj.getPublisherId()).thenReturn(null);
        when(proj.getGenreIds()).thenReturn("1");
        when(proj.getGenreNames()).thenReturn(null);

        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(comicRepository.findByGenreAndYearNative(any(), any(), any(Pageable.class)))
            .thenReturn(List.of(proj));

        List<ComicDto> result = comicService.searchComplex("Action", 2000, 0, 5, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).genres()).isEmpty();
    }

    // ========== create() ==========

    @Test
    void create_whenValid_savesAndInvalidatesCache() {
        ComicRequest request = new ComicRequest("Test Comic", 2000, 1L, 1L, Set.of(1L));

        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(author);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(publisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));
        when(comicRepository.save(any(Comic.class))).thenReturn(comic);
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        ComicDto result = comicService.create(request);

        assertThat(result).isEqualTo(comicDto);
        verify(cacheManager).invalidate();
    }

    @Test
    void create_whenAuthorNotFound_throwsValidationException() {
        ComicRequest request = new ComicRequest("Test", 2000, 99L, 1L, Set.of(1L));

        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(authorRepository.existsById(99L)).thenReturn(false);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(publisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));

        assertThatThrownBy(() -> comicService.create(request))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_whenPublisherNotFound_throwsValidationException() {
        ComicRequest request = new ComicRequest("Test", 2000, 1L, 99L, Set.of(1L));

        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(author);
        when(publisherRepository.existsById(99L)).thenReturn(false);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));

        assertThatThrownBy(() -> comicService.create(request))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_whenGenreNotFound_throwsValidationException() {
        ComicRequest request = new ComicRequest("Test", 2000, 1L, 1L, Set.of(1L, 99L));

        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(author);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(publisher);
        when(genreRepository.findAllById(any())).thenReturn(List.of(genre));

        assertThatThrownBy(() -> comicService.create(request))
            .isInstanceOf(ValidationException.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void create_whenAnnotationValidationErrors_throwsValidationException() {
        ComicRequest request = new ComicRequest("", 2000, 1L, 1L, Set.of(1L));

        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("title");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        Set violations = Set.of(violation);
        when(validator.validate(any())).thenReturn(violations);
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(author);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(publisher);
        when(genreRepository.findAllById(any())).thenReturn(List.of(genre));

        assertThatThrownBy(() -> comicService.create(request))
            .isInstanceOf(ValidationException.class);
    }

    // ========== update() ==========

    @Test
    void update_whenComicExists_updatesAndInvalidates() {
        ComicRequest request = new ComicRequest("Updated", 2001, 1L, 1L, Set.of(1L));

        when(comicRepository.findById(1L)).thenReturn(Optional.of(comic));
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(author);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(publisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));
        when(comicRepository.save(any(Comic.class))).thenReturn(comic);
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        ComicDto result = comicService.update(1L, request);

        assertThat(result).isEqualTo(comicDto);
        verify(cacheManager).invalidate();
    }

    @Test
    void update_whenComicNotFound_throwsException() {
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicService.update(99L,
            new ComicRequest("T", 2000, 1L, 1L, Set.of(1L))))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ========== delete() ==========

    @Test
    void delete_whenExists_deletesAndInvalidates() {
        when(comicRepository.existsById(1L)).thenReturn(true);

        comicService.delete(1L);

        verify(comicRepository).deleteById(1L);
        verify(cacheManager).invalidate();
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(comicRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> comicService.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ========== patch() ==========

    @Test
    void patch_whenAllFieldsProvided_updatesAll() {
        ComicPatchRequest request = new ComicPatchRequest("Patched", 2005, 1L, 1L, Set.of(1L));

        when(comicRepository.findById(1L)).thenReturn(Optional.of(comic));
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(author);
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(publisher);
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));
        when(comicRepository.save(any(Comic.class))).thenReturn(comic);
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        ComicDto result = comicService.patch(1L, request);

        assertThat(result).isEqualTo(comicDto);
        verify(cacheManager).invalidate();
    }

    @Test
    void patch_whenOnlyTitleProvided_updatesOnlyTitle() {
        ComicPatchRequest request = new ComicPatchRequest("New Title", null, null, null, null);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(comic));
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(comicRepository.save(any(Comic.class))).thenReturn(comic);
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        ComicDto result = comicService.patch(1L, request);

        assertThat(result).isEqualTo(comicDto);
        assertThat(comic.getTitle()).isEqualTo("New Title");
        verify(authorRepository, never()).existsById(any());
        verify(publisherRepository, never()).existsById(any());
        verify(genreRepository, never()).findAllById(any());
    }

    @Test
    void patch_whenOnlyReleaseYearProvided_updatesOnlyYear() {
        ComicPatchRequest request = new ComicPatchRequest(null, 2010, null, null, null);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(comic));
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(comicRepository.save(any(Comic.class))).thenReturn(comic);
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        ComicDto result = comicService.patch(1L, request);

        assertThat(result).isEqualTo(comicDto);
        assertThat(comic.getReleaseYear()).isEqualTo(2010);
    }

    @Test
    void patch_whenAuthorIdProvided_andAuthorFound_setsAuthor() {
        ComicPatchRequest request = new ComicPatchRequest(null, null, 1L, null, null);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(comic));
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(authorRepository.getReferenceById(1L)).thenReturn(author);
        when(comicRepository.save(any(Comic.class))).thenReturn(comic);
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        ComicDto result = comicService.patch(1L, request);

        assertThat(result).isEqualTo(comicDto);
        verify(authorRepository).existsById(1L);
    }

    @Test
    void patch_whenPublisherIdProvided_andPublisherFound_setsPublisher() {
        ComicPatchRequest request = new ComicPatchRequest(null, null, null, 1L, null);

        when(comicRepository.findById(1L)).thenReturn(Optional.of(comic));
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(publisherRepository.existsById(1L)).thenReturn(true);
        when(publisherRepository.getReferenceById(1L)).thenReturn(publisher);
        when(comicRepository.save(any(Comic.class))).thenReturn(comic);
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        ComicDto result = comicService.patch(1L, request);

        assertThat(result).isEqualTo(comicDto);
        verify(publisherRepository).existsById(1L);
    }

    @Test
    void patch_whenGenreIdsProvided_andGenresFound_setsGenres() {
        ComicPatchRequest request = new ComicPatchRequest(null, null, null, null, Set.of(1L));

        when(comicRepository.findById(1L)).thenReturn(Optional.of(comic));
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));
        when(comicRepository.save(any(Comic.class))).thenReturn(comic);
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        ComicDto result = comicService.patch(1L, request);

        assertThat(result).isEqualTo(comicDto);
        verify(genreRepository).findAllById(Set.of(1L));
    }

    @Test
    void patch_whenGenreIdsEmptySet_doesNotCallGenreRepository() {
        // extractGenreIds для ComicPatchRequest возвращает пустой Set
        // validateGenres: genreIds.isEmpty() → return Optional.empty()
        ComicPatchRequest request = new ComicPatchRequest("Title", 2000, null, null,
            Collections.emptySet());

        when(comicRepository.findById(1L)).thenReturn(Optional.of(comic));
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(comicRepository.save(any(Comic.class))).thenReturn(comic);
        when(comicMapper.toDto(comic)).thenReturn(comicDto);

        ComicDto result = comicService.patch(1L, request);

        assertThat(result).isNotNull();
        // genreIds != null но isEmpty() → validateGenres возвращает empty
        // genreIds != null → resolved.genres().ifPresent(...) вызывается но Optional.empty()
        verify(genreRepository, never()).findAllById(any());
    }

    @Test
    void patch_whenComicNotFound_throwsException() {
        when(comicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicService.patch(99L,
            new ComicPatchRequest(null, null, null, null, null)))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}