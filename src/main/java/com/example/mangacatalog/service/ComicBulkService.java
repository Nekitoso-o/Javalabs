package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.*;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Genre;
import com.example.mangacatalog.entity.Publisher;
import com.example.mangacatalog.mapper.ComicMapper;
import com.example.mangacatalog.repository.AuthorRepository;
import com.example.mangacatalog.repository.ComicRepository;
import com.example.mangacatalog.repository.GenreRepository;
import com.example.mangacatalog.repository.PublisherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ComicBulkService {

    private static final Logger LOG =
        LoggerFactory.getLogger(ComicBulkService.class);

    private static final String ELEMENT_PREFIX = "Элемент [";

    private final ComicRepository comicRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final GenreRepository genreRepository;
    private final ComicMapper comicMapper;
    private final ApiCacheManager cacheManager;

    public ComicBulkService(ComicRepository comicRepository,
                            AuthorRepository authorRepository,
                            PublisherRepository publisherRepository,
                            GenreRepository genreRepository,
                            ComicMapper comicMapper,
                            ApiCacheManager cacheManager) {
        this.comicRepository = comicRepository;
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.genreRepository = genreRepository;
        this.comicMapper = comicMapper;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public BulkComicResult createBulk(List<ComicRequest> requests) {
        LOG.info("Bulk-создание старт: {} элементов", requests.size());

        // Исправление 2: .toList() вместо .collect(Collectors.toList())
        List<ComicDto> created = IntStream.range(0, requests.size())
            .mapToObj(index -> {
                ComicRequest request = requests.get(index);
                LOG.debug("Bulk [{}]: обработка комикса '{}'",
                    index, request.title());
                Comic comic = buildComic(index, request);
                Comic saved = comicRepository.save(comic);
                LOG.debug("Bulk [{}]: сохранён с ID={}",
                    index, saved.getId());
                return comicMapper.toDto(saved);
            })
            .toList();

        cacheManager.invalidate();
        LOG.info("Bulk-создание завершено: создано {} комиксов",
            created.size());
        return new BulkComicResult(created, created.size());
    }

    private Comic buildComic(int index, ComicRequest request) {


        Author author = Optional.ofNullable(request.authorId())
            .flatMap(authorRepository::findById)
            .orElseThrow(() -> new IllegalArgumentException(
                ELEMENT_PREFIX + index + "]: автор с ID "
                    + request.authorId() + " не найден"));

        Publisher publisher = Optional.ofNullable(request.publisherId())
            .flatMap(publisherRepository::findById)
            .orElseThrow(() -> new IllegalArgumentException(
                ELEMENT_PREFIX + index + "]: издатель с ID "
                    + request.publisherId() + " не найден"));

        Set<Genre> genres = Optional.ofNullable(request.genreIds())
            .filter(ids -> !ids.isEmpty())
            .map(ids -> {
                List<Genre> found = genreRepository.findAllById(ids);

                Set<Long> foundIds = found.stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());

                List<Long> missingIds = ids.stream()
                    .filter(id -> !foundIds.contains(id))
                    .sorted()
                    .toList();

                if (!missingIds.isEmpty()) {
                    throw new IllegalArgumentException(
                        ELEMENT_PREFIX + index + "]: жанры с ID "
                            + missingIds + " не найдены");
                }

                return found.stream().collect(Collectors.toSet());
            })
            .orElseThrow(() -> new IllegalArgumentException(
                ELEMENT_PREFIX + index
                    + "]: список жанров не может быть пустым"));

        Comic comic = new Comic();
        comic.setTitle(request.title());
        comic.setReleaseYear(request.releaseYear());
        comic.setAuthor(author);
        comic.setPublisher(publisher);
        comic.setGenres(genres);
        return comic;
    }
}