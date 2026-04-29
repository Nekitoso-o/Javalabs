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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    private Author author;
    private Publisher publisher;
    private Genre genre;
    private Comic savedComic;
    private ComicDto comicDto;

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

        savedComic = new Comic();
        savedComic.setId(1L);
        savedComic.setTitle("Bulk Comic");

        comicDto = new com.example.mangacatalog.dto.ComicDto(
            1L, "Bulk Comic", 2000, null, null, null);
    }

    @Test
    void createBulk_whenAllValid_returnsResult() {
        ComicRequest request = new ComicRequest("Bulk Comic", 2000, 1L, 1L, Set.of(1L));

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));
        when(comicRepository.save(any(Comic.class))).thenReturn(savedComic);
        when(comicMapper.toDto(savedComic)).thenReturn(comicDto);

        BulkComicResult result = comicBulkService.createBulk(List.of(request));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.created()).hasSize(1);
        verify(cacheManager).invalidate();
    }

    @Test
    void createBulk_multiplePasses_returnsAllCreated() {
        ComicRequest req1 = new ComicRequest("Comic 1", 2001, 1L, 1L, Set.of(1L));
        ComicRequest req2 = new ComicRequest("Comic 2", 2002, 1L, 1L, Set.of(1L));

        Comic saved1 = new Comic();
        saved1.setId(1L);
        Comic saved2 = new Comic();
        saved2.setId(2L);

        ComicDto dto1 = new ComicDto(1L, "Comic 1", 2001, null, null, null);
        ComicDto dto2 = new ComicDto(2L, "Comic 2", 2002, null, null, null);

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        when(genreRepository.findAllById(any())).thenReturn(List.of(genre));
        when(comicRepository.save(any(Comic.class))).thenReturn(saved1, saved2);
        when(comicMapper.toDto(saved1)).thenReturn(dto1);
        when(comicMapper.toDto(saved2)).thenReturn(dto2);

        BulkComicResult result = comicBulkService.createBulk(List.of(req1, req2));

        assertThat(result.successCount()).isEqualTo(2);
        verify(cacheManager).invalidate();
    }

    @Test
    void createBulk_whenAuthorNotFound_throwsIllegalArgumentException() {
        ComicRequest request = new ComicRequest("Comic", 2000, 99L, 1L, Set.of(1L));
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicBulkService.createBulk(List.of(request)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("автор");
    }

    @Test
    void createBulk_whenPublisherNotFound_throwsIllegalArgumentException() {
        ComicRequest request = new ComicRequest("Comic", 2000, 1L, 99L, Set.of(1L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(publisherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicBulkService.createBulk(List.of(request)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("издатель");
    }

    @Test
    void createBulk_whenGenreNotFound_throwsIllegalArgumentException() {
        ComicRequest request = new ComicRequest("Comic", 2000, 1L, 1L, Set.of(1L, 99L));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        // Only one genre found, two requested
        when(genreRepository.findAllById(any())).thenReturn(List.of(genre));

        assertThatThrownBy(() -> comicBulkService.createBulk(List.of(request)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("жанры");
    }

    @Test
    void createBulk_whenGenreIdsNull_throwsIllegalArgumentException() {
        ComicRequest request = new ComicRequest("Comic", 2000, 1L, 1L, null);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));

        assertThatThrownBy(() -> comicBulkService.createBulk(List.of(request)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("жанров не может быть пустым");
    }

    @Test
    void createBulk_whenGenreIdsEmpty_throwsIllegalArgumentException() {
        ComicRequest request = new ComicRequest("Comic", 2000, 1L, 1L, Set.of());
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));

        assertThatThrownBy(() -> comicBulkService.createBulk(List.of(request)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("жанров не может быть пустым");
    }
}