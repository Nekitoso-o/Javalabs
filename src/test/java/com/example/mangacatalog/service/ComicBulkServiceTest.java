package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.BulkComicResult;
import com.example.mangacatalog.dto.ComicDto;
import com.example.mangacatalog.dto.ComicRequest;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Genre;
import com.example.mangacatalog.entity.Publisher;
import com.example.mangacatalog.exception.BulkValidationException;
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

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComicBulkService — unit-тесты")
class ComicBulkServiceTest {

    @Mock private ComicRepository comicRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private PublisherRepository publisherRepository;
    @Mock private GenreRepository genreRepository;
    @Mock private ComicMapper comicMapper;
    @Mock private ApiCacheManager cacheManager;

    @InjectMocks
    private ComicBulkService bulkService;

    private Author author;
    private Publisher publisher;
    private Genre genre;

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setId(1L);
        author.setName("Тестовый автор");

        publisher = new Publisher();
        publisher.setId(1L);
        publisher.setName("Тестовый издатель");

        genre = new Genre();
        genre.setId(1L);
        genre.setName("Фэнтези");
    }

    // ── createBulk (транзакционный режим) ─────────────────────────────────────

    @Test
    @DisplayName("createBulk: все элементы валидны → создаёт все комиксы")
    void createBulk_allValid_createsAll() {
        // given
        List<ComicRequest> requests = List.of(
            new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of(1L)),
            new ComicRequest("Наруто", 1999, 1L, 1L, Set.of(1L))
        );

        when(authorRepository.findAllById(Set.of(1L))).thenReturn(List.of(author));
        when(publisherRepository.findAllById(Set.of(1L))).thenReturn(List.of(publisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));

        Comic savedComic = new Comic();
        savedComic.setId(10L);
        when(comicRepository.save(any(Comic.class))).thenReturn(savedComic);

        ComicDto mockDto = new ComicDto(10L, "Берсерк", 1989, null, null, Set.of());
        when(comicMapper.toDto(any(Comic.class))).thenReturn(mockDto);

        // when
        BulkComicResult result = bulkService.createBulk(requests);

        // then
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.created()).hasSize(2);
        assertThat(result.errors()).isEmpty();
        assertThat(result.errorCount()).isZero();

        verify(comicRepository, times(2)).save(any(Comic.class));
        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("createBulk: один элемент с несуществующим автором → BulkValidationException")
    void createBulk_invalidAuthor_throwsBulkValidationException() {
        // given — автор с ID 99 не существует
        List<ComicRequest> requests = List.of(
            new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of(1L)),
            new ComicRequest("Невалидный", 2000, 99L, 1L, Set.of(1L))
        );

        when(authorRepository.findAllById(Set.of(1L, 99L))).thenReturn(List.of(author));
        when(publisherRepository.findAllById(Set.of(1L))).thenReturn(List.of(publisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));

        // when / then
        assertThatThrownBy(() -> bulkService.createBulk(requests))
            .isInstanceOf(BulkValidationException.class)
            .satisfies(ex -> {
                BulkValidationException bulkEx = (BulkValidationException) ex;
                assertThat(bulkEx.getErrors()).hasSize(1);
                assertThat(bulkEx.getErrors().get(0).index()).isEqualTo(1);
                assertThat(bulkEx.getErrors().get(0).title()).isEqualTo("Невалидный");
                assertThat(bulkEx.getErrors().get(0).error()).contains("99");
            });

        // транзакция откатится, но save мог быть вызван для первого элемента
        verify(cacheManager, never()).invalidate();
    }

    @Test
    @DisplayName("createBulk: пустой список жанров → BulkValidationException")
    void createBulk_emptyGenres_throwsBulkValidationException() {
        // given
        List<ComicRequest> requests = List.of(
            new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of())
        );

        when(authorRepository.findAllById(Set.of(1L))).thenReturn(List.of(author));
        when(publisherRepository.findAllById(Set.of(1L))).thenReturn(List.of(publisher));
        when(genreRepository.findAllById(Set.of())).thenReturn(List.of());

        // when / then
        assertThatThrownBy(() -> bulkService.createBulk(requests))
            .isInstanceOf(BulkValidationException.class)
            .satisfies(ex -> {
                BulkValidationException bulkEx = (BulkValidationException) ex;
                assertThat(bulkEx.getErrors()).hasSize(1);
                assertThat(bulkEx.getErrors().get(0).error())
                    .contains("жанров не может быть пустым");
            });
    }

    @Test
    @DisplayName("createBulk: несуществующий издатель → BulkValidationException")
    void createBulk_invalidPublisher_throwsBulkValidationException() {
        // given
        List<ComicRequest> requests = List.of(
            new ComicRequest("Берсерк", 1989, 1L, 999L, Set.of(1L))
        );

        when(authorRepository.findAllById(Set.of(1L))).thenReturn(List.of(author));
        when(publisherRepository.findAllById(Set.of(999L))).thenReturn(List.of());
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));

        // when / then
        assertThatThrownBy(() -> bulkService.createBulk(requests))
            .isInstanceOf(BulkValidationException.class)
            .satisfies(ex -> {
                BulkValidationException bulkEx = (BulkValidationException) ex;
                assertThat(bulkEx.getErrors().get(0).error()).contains("999");
            });
    }

    @Test
    @DisplayName("createBulk: несколько ошибок в одном запросе → все перечислены")
    void createBulk_multipleErrors_allReported() {
        // given — и автор и издатель не существуют
        List<ComicRequest> requests = List.of(
            new ComicRequest("Невалидный", 2000, 99L, 999L, Set.of(1L))
        );

        when(authorRepository.findAllById(Set.of(99L))).thenReturn(List.of());
        when(publisherRepository.findAllById(Set.of(999L))).thenReturn(List.of());
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));

        // when / then
        assertThatThrownBy(() -> bulkService.createBulk(requests))
            .isInstanceOf(BulkValidationException.class)
            .satisfies(ex -> {
                BulkValidationException bulkEx = (BulkValidationException) ex;
                // Одна запись об ошибке, но содержит обе проблемы
                assertThat(bulkEx.getErrors()).hasSize(1);
                String errorMsg = bulkEx.getErrors().get(0).error();
                assertThat(errorMsg).contains("99");
                assertThat(errorMsg).contains("999");
            });
    }

    // ── createBulkPartial (частичный режим) ───────────────────────────────────

    @Test
    @DisplayName("createBulkPartial: первый валиден, второй нет → первый создан, второй в errors")
    void createBulkPartial_mixedRequests_partialSuccess() {
        // given
        List<ComicRequest> requests = List.of(
            new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of(1L)),
            new ComicRequest("Невалидный", 2000, 99L, 1L, Set.of(1L))
        );

        when(authorRepository.findAllById(Set.of(1L, 99L))).thenReturn(List.of(author));
        when(publisherRepository.findAllById(Set.of(1L))).thenReturn(List.of(publisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));

        Comic savedComic = new Comic();
        savedComic.setId(5L);
        when(bulkService.saveInNewTransaction(any(Comic.class))).thenReturn(savedComic);

        ComicDto mockDto = new ComicDto(5L, "Берсерк", 1989, null, null, Set.of());
        when(comicMapper.toDto(any(Comic.class))).thenReturn(mockDto);

        // when
        BulkComicResult result = bulkService.createBulkPartial(requests);

        // then
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.created()).hasSize(1);
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).index()).isEqualTo(1);
        assertThat(result.errors().get(0).title()).isEqualTo("Невалидный");

        verify(cacheManager).invalidate();
    }

    @Test
    @DisplayName("createBulkPartial: все элементы невалидны → ничего не создано")
    void createBulkPartial_allInvalid_nothingCreated() {
        // given
        List<ComicRequest> requests = List.of(
            new ComicRequest("Первый", 2000, 99L, 1L, Set.of(1L)),
            new ComicRequest("Второй", 2001, 1L, 999L, Set.of(1L))
        );

        when(authorRepository.findAllById(anySet())).thenReturn(List.of(author));
        when(publisherRepository.findAllById(anySet())).thenReturn(List.of(publisher));
        when(genreRepository.findAllById(anySet())).thenReturn(List.of(genre));

        // when
        BulkComicResult result = bulkService.createBulkPartial(requests);

        // then
        assertThat(result.successCount()).isZero();
        assertThat(result.errorCount()).isEqualTo(2);
        verify(comicRepository, never()).save(any());
        verify(cacheManager, never()).invalidate();
    }

    @Test
    @DisplayName("createBulkPartial: все валидны → 201, нет ошибок")
    void createBulkPartial_allValid_allCreated() {
        // given
        List<ComicRequest> requests = List.of(
            new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of(1L))
        );

        when(authorRepository.findAllById(Set.of(1L))).thenReturn(List.of(author));
        when(publisherRepository.findAllById(Set.of(1L))).thenReturn(List.of(publisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));

        Comic savedComic = new Comic();
        savedComic.setId(1L);
        when(bulkService.saveInNewTransaction(any(Comic.class))).thenReturn(savedComic);
        when(comicMapper.toDto(any())).thenReturn(
            new ComicDto(1L, "Берсерк", 1989, null, null, Set.of()));

        // when
        BulkComicResult result = bulkService.createBulkPartial(requests);

        // then
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errorCount()).isZero();
        assertThat(result.errors()).isEmpty();
        verify(cacheManager).invalidate();
    }
}