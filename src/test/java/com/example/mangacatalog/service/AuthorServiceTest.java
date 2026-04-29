package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.AuthorDto;
import com.example.mangacatalog.dto.AuthorRequest;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.mapper.AuthorMapper;
import com.example.mangacatalog.repository.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository repository;

    @Mock
    private AuthorMapper mapper;

    @Mock
    private ApiCacheManager cacheManager;

    @InjectMocks
    private AuthorService authorService;

    private Author author;
    private AuthorDto authorDto;

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setId(1L);
        author.setName("Test Author");

        authorDto = new AuthorDto(1L, "Test Author");
    }

    // getAll()

    @Test
    void getAll_whenCacheHit_returnsCachedValue() {
        List<AuthorDto> cached = List.of(authorDto);
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(cached);

        List<AuthorDto> result = authorService.getAll();

        assertThat(result).isEqualTo(cached);
        verify(repository, never()).findAll();
    }

    @Test
    void getAll_whenCacheMiss_fetchesFromDbAndCaches() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findAll()).thenReturn(List.of(author));
        when(mapper.toDto(author)).thenReturn(authorDto);

        List<AuthorDto> result = authorService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(authorDto);
        verify(cacheManager).put(any(ApiCacheKey.class), any());
    }

    // getById()

    @Test
    void getById_whenCacheHit_returnsCachedValue() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(authorDto);

        AuthorDto result = authorService.getById(1L);

        assertThat(result).isEqualTo(authorDto);
        verify(repository, never()).findById(any());
    }

    @Test
    void getById_whenCacheMiss_fetchesFromDb() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(1L)).thenReturn(Optional.of(author));
        when(mapper.toDto(author)).thenReturn(authorDto);

        AuthorDto result = authorService.getById(1L);

        assertThat(result).isEqualTo(authorDto);
        verify(cacheManager).put(any(ApiCacheKey.class), eq(authorDto));
    }

    @Test
    void getById_whenNotFound_throwsException() {
        when(cacheManager.get(any(ApiCacheKey.class))).thenReturn(null);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.getById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // create()

    @Test
    void create_savesAndInvalidatesCache() {
        AuthorRequest request = new AuthorRequest("New Author");
        Author saved = new Author();
        saved.setId(2L);
        saved.setName("New Author");
        AuthorDto savedDto = new AuthorDto(2L, "New Author");

        when(repository.save(any(Author.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(savedDto);

        AuthorDto result = authorService.create(request);

        assertThat(result).isEqualTo(savedDto);
        verify(cacheManager).invalidate();
    }

    // update()

    @Test
    void update_whenAuthorExists_updatesAndInvalidatesCache() {
        AuthorRequest request = new AuthorRequest("Updated Name");
        Author updated = new Author();
        updated.setId(1L);
        updated.setName("Updated Name");
        AuthorDto updatedDto = new AuthorDto(1L, "Updated Name");

        when(repository.findById(1L)).thenReturn(Optional.of(author));
        when(repository.save(any(Author.class))).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(updatedDto);

        AuthorDto result = authorService.update(1L, request);

        assertThat(result).isEqualTo(updatedDto);
        verify(cacheManager).invalidate();
    }

    @Test
    void update_whenNotFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.update(99L, new AuthorRequest("Name")))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // delete()

    @Test
    void delete_whenAuthorExistsWithNoComics_deletesAndInvalidates() {
        author.getComics(); // ensure field initialized
        when(repository.findById(1L)).thenReturn(Optional.of(author));

        authorService.delete(1L);

        verify(repository).delete(author);
        verify(cacheManager).invalidate();
    }

    @Test
    void delete_whenAuthorHasComics_nullifiesAuthorOnComics() {
        Comic comic1 = new Comic();
        comic1.setAuthor(author);
        Comic comic2 = new Comic();
        comic2.setAuthor(author);

        List<Comic> comics = new ArrayList<>();
        comics.add(comic1);
        comics.add(comic2);

        Author authorWithComics = new Author();
        authorWithComics.setId(1L);
        authorWithComics.setName("Author With Comics");
        // set comics via reflection or use a spy
        // We'll create a custom author with comics list
        Author spyAuthor = spy(new Author());
        when(spyAuthor.getComics()).thenReturn(comics);

        when(repository.findById(1L)).thenReturn(Optional.of(spyAuthor));

        authorService.delete(1L);

        assertThat(comic1.getAuthor()).isNull();
        assertThat(comic2.getAuthor()).isNull();
        verify(repository).delete(spyAuthor);
        verify(cacheManager).invalidate();
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}