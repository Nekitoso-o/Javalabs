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

    private final ComicRepository comicRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final ComicMapper comicMapper;

    @Transactional
    public ComicDto create(ComicDto dto) {
        Comic comic = comicMapper.toEntity(dto);

        if (dto.getAuthor() != null && dto.getAuthor().getId() != null) {
            Author author = authorRepository.findById(dto.getAuthor().getId())
                .orElseThrow(() -> new IllegalArgumentException("Author not found!"));
        }

        if (dto.getPublisher() != null && dto.getPublisher().getId() != null) {
            Publisher publisher = publisherRepository.findById(dto.getPublisher().getId())
                .orElseThrow(() -> new IllegalArgumentException("Publisher not found!"));
            comic.setPublisher(publisher);
        }

        if (dto.getGenres() != null && !dto.getGenres().isEmpty()) {
            Set<Genre> genres = dto.getGenres().stream()
                .map(genreDto -> genreRepository.findById(genreDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Genre with ID " + genreDto.getId() + " not found!")))
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
            .orElseThrow(() -> new IllegalArgumentException("Comic not found!"));
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


    @Transactional(readOnly = true)
    public void demonstrateNPlusOne() {
        log.info("--- СТАРТ: Проблема N+1 ---");
        List<Comic> comics = comicRepository.findAllWithNPlusOneProblem();
        for (Comic c : comics) {
            if (c.getAuthor() != null) {
                log.info("Loaded author: {}", c.getAuthor().getName());
            }
        }
    }

    @Transactional(readOnly = true)
    public void demonstrateEntityGraph() {
        log.info("--- СТАРТ: Решение с @EntityGraph ---");
        List<Comic> comics = comicRepository.findAllWithoutNPlusOne();
        for (Comic c : comics) {
            if (c.getAuthor() != null) {
                log.info("Loaded author: {}", c.getAuthor().getName());
            }
        }
    }


    public void saveDataWithoutTransaction() {
        Publisher pub = new Publisher();
        pub.setName("Test Publisher (No Transaction)");
        publisherRepository.save(pub);

        Comic comic = new Comic();
        comic.setTitle("Test Comic");
        comic.setPublisher(pub);

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
        comic.setPublisher(pub);

        if (true) {
            throw new IllegalStateException("Внезапная ошибка при сохранении! Всё будет откатано.");
        }
        comicRepository.save(comic);
    }
}