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
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ComicService {

    private static final Logger LOG = LoggerFactory.getLogger(ComicService.class);
    private static final String COMIC_NOT_FOUND_MSG = "Комикс с ID %s не найден!";
    private static final String AUTHOR_NOT_FOUND_MSG = "Автор с ID %s не найден!";
    private static final String PUBLISHER_NOT_FOUND_MSG = "Издатель с ID %s не найден!";
    private static final String GENRE_NOT_FOUND_MSG = "Жанры с ID %s не найдены!";

    private final ComicRepository comicRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final ComicMapper comicMapper;
    private final ApiCacheManager cacheManager;
    private final Validator validator;

    public ComicService(ComicRepository comicRepository,
                        PublisherRepository publisherRepository,
                        AuthorRepository authorRepository,
                        GenreRepository genreRepository,
                        ComicMapper comicMapper,
                        ApiCacheManager cacheManager,
                        Validator validator) {
        this.comicRepository = comicRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.comicMapper = comicMapper;
        this.cacheManager = cacheManager;
        this.validator = validator;
    }

// ========== Методы чтения ==========

    @SuppressWarnings("unchecked")
    public List<ComicDto> getAll() {
        ApiCacheKey key = new ApiCacheKey("getAllComics");
        Object cached = cacheManager.get(key);
        if (cached != null) {
            return (List<ComicDto>) cached;
        }

        List<ComicDto> result = comicRepository.findAll().stream()
            .map(comicMapper::toDto)
            .toList();
        cacheManager.put(key, result);
        return result;
    }

    public ComicDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getComicById", id);
        Object cached = cacheManager.get(key);
        if (cached != null) {
            return (ComicDto) cached;
        }

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
        if (cached != null) {
            return (List<ComicDto>) cached;
        }

        List<ComicDto> result = comicRepository.findByTitleContainingIgnoreCase(title)
            .stream()
            .map(comicMapper::toDto)
            .toList();
        cacheManager.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ComicDto> getComicsByAuthor(Long authorId) {
        ApiCacheKey key = new ApiCacheKey("getComicsByAuthor", authorId);
        Object cached = cacheManager.get(key);
        if (cached != null) {
            return (List<ComicDto>) cached;
        }

        List<ComicDto> result = comicRepository.findByAuthorId(authorId)
            .stream()
            .map(comicMapper::toDto)
            .toList();
        cacheManager.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ComicDto> searchComplex(String genreName, Integer minYear, int page, int size, boolean useNative) {
        ApiCacheKey key = new ApiCacheKey("searchComplex", genreName, minYear, page, size);
        Object cached = cacheManager.get(key);
        if (cached != null) {
            return (List<ComicDto>) cached;
        }

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

// ========== Методы записи (с полной валидацией) ==========

    @Transactional
    public ComicDto create(ComicRequest request) {
        ResolvedEntities resolved = validateAndResolve(request);

        Comic comic = new Comic();
        comic.setTitle(request.title());
        comic.setReleaseYear(request.releaseYear());
        comic.setAuthor(resolved.author());
        comic.setPublisher(resolved.publisher());
        comic.setGenres(resolved.genres());

        Comic savedComic = comicRepository.save(comic);
        cacheManager.invalidate();
        return comicMapper.toDto(savedComic);
    }

    @Transactional
    public ComicDto update(Long id, ComicRequest request) {
        Comic existing = comicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(COMIC_NOT_FOUND_MSG, id)));

        ResolvedEntities resolved = validateAndResolve(request);

        existing.setTitle(request.title());
        existing.setReleaseYear(request.releaseYear());
        existing.setAuthor(resolved.author());
        existing.setPublisher(resolved.publisher());
        existing.setGenres(resolved.genres());

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

        ResolvedEntities resolved = validateAndResolve(request);

        if (request.title() != null) {
            existing.setTitle(request.title());
        }
        if (request.releaseYear() != null) {
            existing.setReleaseYear(request.releaseYear());
        }
        if (resolved.author() != null) {
            existing.setAuthor(resolved.author());
        }
        if (resolved.publisher() != null) {
            existing.setPublisher(resolved.publisher());
        }
        if (resolved.genres() != null) {
            existing.setGenres(resolved.genres());
        }

        Comic updatedComic = comicRepository.save(existing);
        cacheManager.invalidate();
        return comicMapper.toDto(updatedComic);
    }

// ========== Вспомогательные методы для валидации ==========


    private ResolvedEntities validateAndResolve(Object request) {
        Map<String, String> errors = new LinkedHashMap<>();

        // 1. Синтаксическая валидация (через аннотации @NotBlank, @NotNull и т.д.)
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        for (ConstraintViolation<Object> violation : violations) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }

        // 2. Извлекаем ID из соответствующего DTO
        Long authorId = null;
        Long publisherId = null;
        Set<Long> genreIds = null;

        if (request instanceof ComicRequest cr) {
            authorId = cr.authorId();
            publisherId = cr.publisherId();
            genreIds = cr.genreIds();
        } else if (request instanceof ComicPatchRequest pr) {
            authorId = pr.authorId();
            publisherId = pr.publisherId();
            genreIds = pr.genreIds();
        }

        // 3. Бизнес-валидация: проверяем существование ID
        Author author = null;
        if (authorId != null) {
            author = authorRepository.findById(authorId).orElse(null);
            if (author == null) {
                errors.put("authorId", String.format(AUTHOR_NOT_FOUND_MSG, authorId));
            }
        }

        Publisher publisher = null;
        if (publisherId != null) {
            publisher = publisherRepository.findById(publisherId).orElse(null);
            if (publisher == null) {
                errors.put("publisherId", String.format(PUBLISHER_NOT_FOUND_MSG, publisherId));
            }
        }

        Set<Genre> genres = null;
        if (genreIds != null) {
            genres = new HashSet<>();
            List<Long> notFoundGenreIds = new ArrayList<>();
            for (Long genreId : genreIds) {
                Genre genre = genreRepository.findById(genreId).orElse(null);
                if (genre == null) {
                    notFoundGenreIds.add(genreId);
                } else {
                    genres.add(genre);
                }
            }
            if (!notFoundGenreIds.isEmpty()) {
                errors.put("genreIds", String.format(GENRE_NOT_FOUND_MSG, notFoundGenreIds));
            }
        }

        // 4. Если есть хоть одна ошибка - выбрасываем исключение со всеми
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        return new ResolvedEntities(author, publisher, genres);
    }


    private record ResolvedEntities(Author author, Publisher publisher, Set<Genre> genres) {
    }
}