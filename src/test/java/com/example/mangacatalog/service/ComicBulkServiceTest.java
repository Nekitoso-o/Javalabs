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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComicBulkService — unit-тесты")
class ComicBulkServiceTest {

    @Mock private ComicRepository comicRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private PublisherRepository publisherRepository;
    @Mock private GenreRepository genreRepository;


    private ComicMapper comicMapper;
    private ApiCacheManager cacheManager;

    private ComicBulkService bulkService;

    private Author author;
    private Publisher publisher;
    private Genre genre;

    @BeforeEach
    void setUp() {
        comicMapper = new ComicMapper(new AuthorMapper(), new PublisherMapper(), new GenreMapper());
        cacheManager = new ApiCacheManager();

        bulkService = new ComicBulkService(
            comicRepository,
            authorRepository,
            publisherRepository,
            genreRepository,
            comicMapper,
            cacheManager
        );

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

    @Test
    @DisplayName("createBulk: все элементы валидны → создаёт все комиксы")
    void createBulk_allValid_createsAll() {
        List<ComicRequest> requests = List.of(
            new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of(1L)),
            new ComicRequest("Наруто", 1999, 1L, 1L, Set.of(1L))
        );

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));

        when(comicRepository.save(any(Comic.class))).thenAnswer(invocation -> {
            Comic c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        // when
        BulkComicResult result = bulkService.createBulk(requests);

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.created()).hasSize(2);
        assertThat(result.created().get(0).title()).isEqualTo("Берсерк");

        verify(comicRepository, times(2)).save(any(Comic.class));
    }

    @Test
    @DisplayName("createBulk: несуществующий автор → выбрасывает IllegalArgumentException")
    void createBulk_invalidAuthor_throwsIllegalArgumentException() {
        // given
        List<ComicRequest> requests = List.of(
            new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of(1L)),
            new ComicRequest("Невалидный", 2000, 99L, 1L, Set.of(1L))
        );

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));
        when(comicRepository.save(any())).thenReturn(new Comic());

        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bulkService.createBulk(requests))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("автор с ID 99 не найден");
    }

    @Test
    @DisplayName("createBulk: несуществующий издатель → выбрасывает IllegalArgumentException")
    void createBulk_invalidPublisher_throwsIllegalArgumentException() {
        List<ComicRequest> requests = List.of(
            new ComicRequest("Берсерк", 1989, 1L, 999L, Set.of(1L))
        );

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(publisherRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bulkService.createBulk(requests))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("издатель с ID 999 не найден");
    }

    @Test
    @DisplayName("createBulk: пустой список жанров → выбрасывает IllegalArgumentException")
    void createBulk_emptyGenres_throwsIllegalArgumentException() {
        List<ComicRequest> requests = List.of(
            new ComicRequest("Берсерк", 1989, 1L, 1L, Set.of())
        );

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));

        assertThatThrownBy(() -> bulkService.createBulk(requests))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("список жанров не может быть пустым");
    }
}