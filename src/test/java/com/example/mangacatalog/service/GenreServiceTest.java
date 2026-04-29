package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.GenreDto;
import com.example.mangacatalog.dto.GenreRequest;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Genre;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.mapper.GenreMapper;
import com.example.mangacatalog.repository.GenreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    private GenreRepository repository;

    @Mock
    private GenreMapper mapper;

    @Mock
    private ApiCacheManager cacheManager;

    @InjectMocks
    private GenreService genreService;

    private Genre genre;
    private GenreDto genreDto;

    @BeforeEach
    void setUp() {
        genre = new Genre();
        genre.setId(1L);
        genre.setName("Action");

        genreDto = new GenreDto(1L, "Action");
    }

    @Test
    void getAll_whenCacheHit_returnsCached() {
        List<GenreDto> cached = List.of(genreDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<GenreDto> result = genreService.getAll();

        assertThat(result).isEqualTo(cached);
        verify(repository, never()).findAll();
    }

    @Test
    void getAll_whenCacheMiss_fetchesAndCaches() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(List.of(genre));
        when(mapper.toDto(genre)).thenReturn(genreDto);

        List<GenreDto> result = genreService.getAll();

        assertThat(result).containsExactly(genreDto);
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    @Test
    void getById_whenCacheHit_returnsCached() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(genreDto);

        GenreDto result = genreService.getById(1L);

        assertThat(result).isEqualTo(genreDto);
        verify(repository, never()).findById(any());
    }

    @Test
    void getById_whenCacheMiss_fetchesFromDb() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(1L)).thenReturn(Optional.of(genre));
        when(mapper.toDto(genre)).thenReturn(genreDto);

        GenreDto result = genreService.getById(1L);

        assertThat(result).isEqualTo(genreDto);
        verify(cacheManager).put(any(ApiCacheKey.class), eq(genreDto));
    }

    @Test
    void getById_whenNotFound_throwsException() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.getById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    void create_savesAndInvalidatesCache() {
        GenreRequest request = new GenreRequest("Horror");
        Genre saved = new Genre();
        saved.setId(2L);
        saved.setName("Horror");
        GenreDto savedDto = new GenreDto(2L, "Horror");

        when(repository.save(any(Genre.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(savedDto);

        GenreDto result = genreService.create(request);

        assertThat(result).isEqualTo(savedDto);
        verify(cacheManager).invalidate();
    }

    @Test
    void update_whenFound_updatesAndInvalidates() {
        GenreRequest request = new GenreRequest("Updated");
        Genre updated = new Genre();
        updated.setId(1L);
        updated.setName("Updated");
        GenreDto updatedDto = new GenreDto(1L, "Updated");

        when(repository.findById(1L)).thenReturn(Optional.of(genre));
        when(repository.save(any(Genre.class))).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(updatedDto);

        GenreDto result = genreService.update(1L, request);

        assertThat(result).isEqualTo(updatedDto);
        verify(cacheManager).invalidate();
    }

    @Test
    void update_whenNotFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.update(99L, new GenreRequest("x")))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_whenGenreHasNoComics_deletesAndInvalidates() {
        when(repository.findById(1L)).thenReturn(Optional.of(genre));

        genreService.delete(1L);

        verify(repository).delete(genre);
        verify(cacheManager).invalidate();
    }

    @Test
    void delete_whenGenreHasComics_removesGenreFromComics() {
        Genre genreWithComics = spy(new Genre());
        genreWithComics.setId(1L);
        genreWithComics.setName("Action");

        Comic comic = new Comic();
        Set<Genre> genres = new HashSet<>();
        genres.add(genreWithComics);
        comic.setGenres(genres);

        List<Comic> comics = new ArrayList<>();
        comics.add(comic);
        when(genreWithComics.getComics()).thenReturn(comics);

        when(repository.findById(1L)).thenReturn(Optional.of(genreWithComics));

        genreService.delete(1L);

        assertThat(comic.getGenres()).doesNotContain(genreWithComics);
        verify(repository).delete(genreWithComics);
        verify(cacheManager).invalidate();
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}