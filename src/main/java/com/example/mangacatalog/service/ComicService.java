package com.example.mangacatalog.service;

import com.example.mangacatalog.dto.ComicDto;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Genre;
import com.example.mangacatalog.entity.Publisher;
import com.example.mangacatalog.mapper.ComicMapper;
import com.example.mangacatalog.repository.AuthorRepository;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.GenreRepository;
import com.example.mangacatalog.repository.PublisherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComicService {

    private static final String COMIC_NOT_FOUND = "Comic not found!";
    private static final String NOT_FOUND_SUFFIX = " not found!";
    private static final String AUTHOR_NOT_FOUND = "Author not found!";
    private static final String PUBLISHER_NOT_FOUND = "Publisher not found!";
    private static final String GENRE_NOT_FOUND = "Genre not found!";

    private final ComicRepository comicRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final ComicMapper comicMapper;

    @Transactional
    public ComicDto create(ComicDto dto) {
        Comic comic = comicMapper.toEntity(dto);

        if (dto.getAuthors() != null && !dto.getAuthors().isEmpty()) {
            Set<Author> authors = dto.getAuthors().stream()
                .map(authorDto -> authorRepository.findById(authorDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Author with ID " + authorDto.getId() + NOT_FOUND_SUFFIX)))
                .collect(Collectors.toSet());
            comic.setAuthors(authors);
        }

        if (dto.getPublishers() != null && !dto.getPublishers().isEmpty()) {
            Set<Publisher> publishers = dto.getPublishers().stream()
                .map(pubDto -> publisherRepository.findById(pubDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Publisher with ID " + pubDto.getId() + NOT_FOUND_SUFFIX)))
                .collect(Collectors.toSet());
            comic.setPublishers(publishers);
        }

        if (dto.getGenres() != null && !dto.getGenres().isEmpty()) {
            Set<Genre> genres = dto.getGenres().stream()
                .map(genreDto -> genreRepository.findById(genreDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Genre with ID " + genreDto.getId() + NOT_FOUND_SUFFIX)))
                .collect(Collectors.toSet());
            comic.setGenres(genres);
        }

        Comic savedComic = comicRepository.save(comic);
        return comicMapper.toDto(savedComic);
    }

    public List<ComicDto> getAll() {
        return comicRepository.findAll().stream()
            .map(comicMapper::toDto)
            .toList();
    }

    public ComicDto getById(Long id) {
        Comic comic = comicRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(COMIC_NOT_FOUND));
        return comicMapper.toDto(comic);
    }

    public List<ComicDto> searchByTitle(String title) {
        return comicRepository.findByTitleContainingIgnoreCase(title).stream()
            .map(comicMapper::toDto)
            .toList();
    }

    @Transactional
    public void delete(Long id) {
        comicRepository.deleteById(id);
    }

    @Transactional
    public ComicDto update(Long id, ComicDto dto) {
        Comic existing = comicRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(COMIC_NOT_FOUND));

        existing.setTitle(dto.getTitle());
        existing.setReleaseYear(dto.getReleaseYear());

        if (dto.getAuthors() != null) {
            Set<Author> authors = dto.getAuthors().stream()
                .map(a -> authorRepository.findById(a.getId())
                    .orElseThrow(() -> new IllegalArgumentException(AUTHOR_NOT_FOUND)))
                .collect(Collectors.toSet());
            existing.setAuthors(authors);
        }

        if (dto.getPublishers() != null) {
            Set<Publisher> publishers = dto.getPublishers().stream()
                .map(p -> publisherRepository.findById(p.getId())
                    .orElseThrow(() -> new IllegalArgumentException(PUBLISHER_NOT_FOUND)))
                .collect(Collectors.toSet());
            existing.setPublishers(publishers);
        }

        if (dto.getGenres() != null) {
            Set<Genre> genres = dto.getGenres().stream()
                .map(g -> genreRepository.findById(g.getId())
                    .orElseThrow(() -> new IllegalArgumentException(GENRE_NOT_FOUND)))
                .collect(Collectors.toSet());
            existing.setGenres(genres);
        }

        return comicMapper.toDto(comicRepository.save(existing));
    }

    @Transactional
    public ComicDto patch(Long id, ComicDto dto) {
        Comic existing = comicRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(COMIC_NOT_FOUND));

        if (dto.getTitle() != null) {
            existing.setTitle(dto.getTitle());
        }
        if (dto.getReleaseYear() != null) {
            existing.setReleaseYear(dto.getReleaseYear());
        }

        if (dto.getAuthors() != null && !dto.getAuthors().isEmpty()) {
            Set<Author> authors = dto.getAuthors().stream()
                .map(a -> authorRepository.findById(a.getId())
                    .orElseThrow(() -> new IllegalArgumentException(AUTHOR_NOT_FOUND)))
                .collect(Collectors.toSet());
            existing.setAuthors(authors);
        }

        if (dto.getPublishers() != null && !dto.getPublishers().isEmpty()) {
            Set<Publisher> publishers = dto.getPublishers().stream()
                .map(p -> publisherRepository.findById(p.getId())
                    .orElseThrow(() -> new IllegalArgumentException(PUBLISHER_NOT_FOUND)))
                .collect(Collectors.toSet());
            existing.setPublishers(publishers);
        }

        if (dto.getGenres() != null && !dto.getGenres().isEmpty()) {
            Set<Genre> genres = dto.getGenres().stream()
                .map(g -> genreRepository.findById(g.getId())
                    .orElseThrow(() -> new IllegalArgumentException(GENRE_NOT_FOUND)))
                .collect(Collectors.toSet());
            existing.setGenres(genres);
        }

        return comicMapper.toDto(comicRepository.save(existing));
    }


    @Transactional(readOnly = true)
    public void demonstrateNPlusOne() {
        log.info("--- СТАРТ: Проблема N+1 ---");
        List<Comic> comics = comicRepository.findAllWithNPlusOneProblem();
        for (Comic c : comics) {
            if (c.getAuthors() != null) {
                for (Author a : c.getAuthors()) {
                    log.info("Loaded author: {}", a.getName());
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public void demonstrateEntityGraph() {
        log.info("--- СТАРТ: Решение с @EntityGraph ---");
        List<Comic> comics = comicRepository.findAllWithoutNPlusOne();
        for (Comic c : comics) {
            if (c.getAuthors() != null) {
                for (Author a : c.getAuthors()) {
                    log.info("Loaded author: {}", a.getName());
                }
            }
        }
    }


    public void saveDataWithoutTransaction() {
        Publisher pub = new Publisher();
        pub.setName("Test Publisher (No Transaction)");
        publisherRepository.save(pub);

        Comic comic = new Comic();
        comic.setTitle("Test Comic");
        comic.setPublishers(Set.of(pub));

        if (true) {
            throw new IllegalStateException("Внезапная ошибка при сохранении!");
        }
        comicRepository.save(comic);
    }

    @Transactional
    public void saveDataWithTransaction() {
        Publisher pub = new Publisher();
        pub.setName("Test Publisher (With Transaction)");
        publisherRepository.save(pub);

        Comic comic = new Comic();
        comic.setTitle("Test Comic");
        comic.setPublishers(Set.of(pub));

        if (true) {
            throw new IllegalStateException("Внезапная ошибка при сохранении! Всё будет откатано.");
        }
        comicRepository.save(comic);
    }
}