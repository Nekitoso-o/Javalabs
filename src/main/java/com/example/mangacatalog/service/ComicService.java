package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.*;
import com.example.mangacatalog.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ComicService {

    private static final Logger LOG = LoggerFactory.getLogger(ComicService.class);
    private static final String COMIC_NOT_FOUND_MSG = "Комикс с ID %s не найден!";
    private static final String AUTHOR_NOT_FOUND_MSG = "Автор с ID %s не найден!";
    private static final String PUBLISHER_NOT_FOUND_MSG = "Издатель с ID %s не найден!";
    private static final String GENRE_NOT_FOUND_MSG = "Жанр c ID %s не найден!";

    private final ComicRepository comicRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final ComicMapper comicMapper;
    private final ApiCacheManager cacheManager;

    public ComicService(ComicRepository comicRepository,
                        PublisherRepository publisherRepository,
                        AuthorRepository authorRepository,
                        GenreRepository genreRepository,
                        ComicMapper comicMapper,
                        ApiCacheManager cacheManager) {
        this.comicRepository = comicRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.comicMapper = comicMapper;
        this.cacheManager = cacheManager;
    }

    @SuppressWarnings("unchecked")
    public List<ComicDto> getAll() {
        ApiCacheKey key = new ApiCacheKey("getAllComics");

        Object cached = cacheManager.get(key);
        if (cached != null) return (List<ComicDto>) cached;

        List<ComicDto> result = comicRepository.findAll().stream().map(comicMapper::toDto).toList();
        cacheManager.put(key, result);
        return result;
    }

    public ComicDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getComicById", id);

        Object cached = cacheManager.get(key);
        if (cached != null) return (ComicDto) cached;

        LOG.info("Запрос к БД для ID: {}", id);
        Comic comic = comicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(COMIC_NOT_FOUND_MSG, id)));
        ComicDto result = comicMapper.toDto(comic);
        cacheManager.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ComicDto> searchByTitle(String title) {
        ApiCacheKey key = new ApiCacheKey("searchByTitle", title);

        Object cached = cacheManager.get(key);
        if (cached != null) return (List<ComicDto>) cached;

        List<ComicDto> result = comicRepository.findByTitleContainingIgnoreCase(title)
            .stream().map(comicMapper::toDto).toList();
        cacheManager.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ComicDto> getComicsByAuthor(Long authorId) {
        ApiCacheKey key = new ApiCacheKey("getComicsByAuthor", authorId);

        Object cached = cacheManager.get(key);
        if (cached != null) return (List<ComicDto>) cached;

        List<ComicDto> result = comicRepository.findByAuthorId(authorId)
            .stream().map(comicMapper::toDto).toList();
        cacheManager.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ComicDto> searchComplex(String genreName, Integer minYear, int page, int size, boolean useNative) {
        ApiCacheKey key = new ApiCacheKey("searchComplex", genreName, minYear, page, size);

        Object cached = cacheManager.get(key);
        if (cached != null) return (List<ComicDto>) cached;

        LOG.info("Запрос к БД (useNative={})", useNative ? "true" : "false");
        Pageable pageable = PageRequest.of(page, size);
        List<ComicDto> dtoList;

        if (useNative) {
            List<ComicNativeProjection> projectionList = comicRepository.findByGenreAndYearNative(genreName, minYear, pageable);
            dtoList = projectionList.stream().map(this::mapProjectionToDto).toList();
        } else {
            List<Comic> comicList = comicRepository.findByGenreAndYearJpql(genreName, minYear, pageable);
            dtoList = comicList.stream().map(comicMapper::toDto).toList();
        }

        cacheManager.put(key, dtoList);
        return dtoList;
    }

    private ComicDto mapProjectionToDto(ComicNativeProjection proj) {
        AuthorDto author = proj.getAuthorId() != null ?
            new AuthorDto(proj.getAuthorId(), proj.getAuthorName()) : null;
        PublisherDto publisher = proj.getPublisherId() != null ?
            new PublisherDto(proj.getPublisherId(), proj.getPublisherName()) : null;

        Set<GenreDto> genres = new HashSet<>();
        if (proj.getGenreIds() != null && proj.getGenreNames() != null) {
            String[] ids = proj.getGenreIds().split(",");
            String[] names = proj.getGenreNames().split(",");
            for (int i = 0; i < ids.length; i++) {
                genres.add(new GenreDto(Long.valueOf(ids[i]), names[i].trim()));
            }
        }
        return new ComicDto(proj.getId(), proj.getTitle(), proj.getReleaseYear(), author, publisher, genres);
    }

    @Transactional
    public ComicDto create(ComicRequest request) {
        Comic comic = new Comic();
        comic.setTitle(request.title());
        comic.setReleaseYear(request.releaseYear());

        Author author = authorRepository.findById(request.authorId())
            .orElseThrow(() -> new ResourceNotFoundException(String.format(AUTHOR_NOT_FOUND_MSG, request.authorId())));
        comic.setAuthor(author);

        Publisher publisher = publisherRepository.findById(request.publisherId())
            .orElseThrow(() -> new ResourceNotFoundException(String.format(PUBLISHER_NOT_FOUND_MSG, request.publisherId())));
        comic.setPublisher(publisher);

        if (request.genreIds() != null && !request.genreIds().isEmpty()) {
            Set<Genre> genres = request.genreIds().stream()
                .map(id -> genreRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(GENRE_NOT_FOUND_MSG, id))))
                .collect(Collectors.toSet());
            comic.setGenres(genres);
        }

        Comic savedComic = comicRepository.save(comic);
        cacheManager.invalidate();
        return comicMapper.toDto(savedComic);
    }

    @Transactional
    public ComicDto update(Long id, ComicRequest request) {
        Comic existing = comicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(COMIC_NOT_FOUND_MSG, id)));

        existing.setTitle(request.title());
        existing.setReleaseYear(request.releaseYear());

        Author author = authorRepository.findById(request.authorId())
            .orElseThrow(() -> new ResourceNotFoundException(String.format(AUTHOR_NOT_FOUND_MSG, request.authorId())));
        existing.setAuthor(author);

        Publisher publisher = publisherRepository.findById(request.publisherId())
            .orElseThrow(() -> new ResourceNotFoundException(String.format(PUBLISHER_NOT_FOUND_MSG, request.publisherId())));
        existing.setPublisher(publisher);

        if (request.genreIds() != null) {
            Set<Genre> genres = request.genreIds().stream()
                .map(gId -> genreRepository.findById(gId)
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(GENRE_NOT_FOUND_MSG, gId))))
                .collect(Collectors.toSet());
            existing.setGenres(genres);
        }

        Comic updatedComic = comicRepository.save(existing);
        cacheManager.invalidate();
        return comicMapper.toDto(updatedComic);
    }

    @Transactional
    public void delete(Long id) {
        if (!comicRepository.existsById(id)) {
            throw new ResourceNotFoundException(String.format(COMIC_NOT_FOUND_MSG, id));
        }
        comicRepository.deleteById(id);
        cacheManager.invalidate();
    }

    @Transactional
    public ComicDto patch(Long id, ComicPatchRequest request) {
        Comic existing = comicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(COMIC_NOT_FOUND_MSG, id)));

        if (request.title() != null) {
            existing.setTitle(request.title());
        }
        if (request.releaseYear() != null) {
            existing.setReleaseYear(request.releaseYear());
        }
        if (request.authorId() != null) {
            Author author = authorRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(AUTHOR_NOT_FOUND_MSG, request.authorId())));
            existing.setAuthor(author);
        }
        if (request.publisherId() != null) {
            Publisher publisher = publisherRepository.findById(request.publisherId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PUBLISHER_NOT_FOUND_MSG, request.publisherId())));
            existing.setPublisher(publisher);
        }
        if (request.genreIds() != null) {
            Set<Genre> genres = request.genreIds().stream()
                .map(gId -> genreRepository.findById(gId)
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(GENRE_NOT_FOUND_MSG, gId))))
                .collect(Collectors.toSet());
            existing.setGenres(genres);
        }

        Comic updatedComic = comicRepository.save(existing);
        cacheManager.invalidate();
        return comicMapper.toDto(updatedComic);
    }
}