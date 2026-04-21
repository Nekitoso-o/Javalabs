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

import java.util.*;
import java.util.stream.Collectors;

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



    @SuppressWarnings("unchecked")
    public List<ComicDto> getAll() {
        ApiCacheKey key = new ApiCacheKey("getAllComics");
        Object cached = cacheManager.get(key);
        if (cached != null) return (List<ComicDto>) cached;

        List<ComicDto> result = comicRepository.findAll()
            .stream()
            .map(comicMapper::toDto)
            .toList();
        cacheManager.put(key, result);
        return result;
    }

    public ComicDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getComicById", id);
        Object cached = cacheManager.get(key);
        if (cached != null) return (ComicDto) cached;

        LOG.info("Запрос к БД для ID: {}", id);
        Comic comic = comicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(COMIC_NOT_FOUND_MSG, id)));
        ComicDto result = comicMapper.toDto(comic);
        cacheManager.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ComicDto> searchByTitle(String title) {
        ApiCacheKey key = new ApiCacheKey("searchByTitle", title);
        Object cached = cacheManager.get(key);
        if (cached != null) return (List<ComicDto>) cached;

        List<ComicDto> result = comicRepository
            .findByTitleContainingIgnoreCase(title)
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
        if (cached != null) return (List<ComicDto>) cached;

        List<ComicDto> result = comicRepository.findByAuthorId(authorId)
            .stream()
            .map(comicMapper::toDto)
            .toList();
        cacheManager.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ComicDto> searchComplex(String genreName, Integer minYear,
                                        int page, int size, boolean useNative) {
        ApiCacheKey key = new ApiCacheKey("searchComplex", genreName, minYear, page, size);
        Object cached = cacheManager.get(key);
        if (cached != null) return (List<ComicDto>) cached;

        LOG.info("Запрос к БД (useNative={})", useNative ? "true" : "false");
        Pageable pageable = PageRequest.of(page, size);
        List<ComicDto> dtoList;

        if (useNative) {
            List<ComicNativeProjection> projectionList =
                comicRepository.findByGenreAndYearNative(genreName, minYear, pageable);
            dtoList = projectionList.stream().map(this::mapProjectionToDto).toList();
        } else {
            List<Comic> comicList =
                comicRepository.findByGenreAndYearJpql(genreName, minYear, pageable);
            dtoList = comicList.stream().map(comicMapper::toDto).toList();
        }

        cacheManager.put(key, dtoList);
        return dtoList;
    }

    private ComicDto mapProjectionToDto(ComicNativeProjection proj) {
        AuthorDto author = proj.getAuthorId() != null
            ? new AuthorDto(proj.getAuthorId(), proj.getAuthorName()) : null;
        PublisherDto publisher = proj.getPublisherId() != null
            ? new PublisherDto(proj.getPublisherId(), proj.getPublisherName()) : null;

        Set<GenreDto> genres = new HashSet<>();
        if (proj.getGenreIds() != null && proj.getGenreNames() != null) {
            String[] ids = proj.getGenreIds().split(",");
            String[] names = proj.getGenreNames().split(",");
            for (int i = 0; i < ids.length; i++) {
                genres.add(new GenreDto(Long.valueOf(ids[i]), names[i].trim()));
            }
        }
        return new ComicDto(proj.getId(), proj.getTitle(),
            proj.getReleaseYear(), author, publisher, genres);
    }


    @Transactional
    public ComicDto create(ComicRequest request) {
        ResolvedEntities resolved = validateAndResolve(request);

        Comic comic = new Comic();
        comic.setTitle(request.title());
        comic.setReleaseYear(request.releaseYear());
        comic.setAuthor(resolved.author().orElse(null));
        comic.setPublisher(resolved.publisher().orElse(null));
        comic.setGenres(resolved.genres().orElseGet(HashSet::new));

        Comic savedComic = comicRepository.save(comic);
        cacheManager.invalidate();
        return comicMapper.toDto(savedComic);
    }

    @Transactional
    public ComicDto update(Long id, ComicRequest request) {
        Comic existing = comicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(COMIC_NOT_FOUND_MSG, id)));

        ResolvedEntities resolved = validateAndResolve(request);

        existing.setTitle(request.title());
        existing.setReleaseYear(request.releaseYear());
        existing.setAuthor(resolved.author().orElse(null));
        existing.setPublisher(resolved.publisher().orElse(null));
        existing.setGenres(resolved.genres().orElseGet(HashSet::new));

        Comic updatedComic = comicRepository.save(existing);
        cacheManager.invalidate();
        return comicMapper.toDto(updatedComic);
    }

    @Transactional
    public void delete(Long id) {
        if (!comicRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                String.format(COMIC_NOT_FOUND_MSG, id));
        }
        comicRepository.deleteById(id);
        cacheManager.invalidate();
    }

    @Transactional
    public ComicDto patch(Long id, ComicPatchRequest request) {
        Comic existing = comicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(COMIC_NOT_FOUND_MSG, id)));

        ResolvedEntities resolved = validateAndResolve(request);

        if (request.title() != null) {
            existing.setTitle(request.title());
        }
        if (request.releaseYear() != null) {
            existing.setReleaseYear(request.releaseYear());
        }
        if (request.authorId() != null) {
            resolved.author().ifPresent(existing::setAuthor);
        }
        if (request.publisherId() != null) {
            resolved.publisher().ifPresent(existing::setPublisher);
        }
        if (request.genreIds() != null) {
            resolved.genres().ifPresent(existing::setGenres);
        }

        Comic updatedComic = comicRepository.save(existing);
        cacheManager.invalidate();
        return comicMapper.toDto(updatedComic);
    }


    private ResolvedEntities validateAndResolve(Object request) {
        Map<String, String> errors = new LinkedHashMap<>();

        collectAnnotationErrors(request, errors);

        Long authorId = extractAuthorId(request);
        Long publisherId = extractPublisherId(request);
        Set<Long> genreIds = extractGenreIds(request);

        Optional<Author> author = validateAuthor(authorId, errors);
        Optional<Publisher> publisher = validatePublisher(publisherId, errors);
        Optional<Set<Genre>> genres = validateGenres(genreIds, errors);

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        return new ResolvedEntities(author, publisher, genres);
    }

    private void collectAnnotationErrors(Object request, Map<String, String> errors) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        for (ConstraintViolation<Object> violation : violations) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
    }

    private Long extractAuthorId(Object request) {
        if (request instanceof ComicRequest cr) return cr.authorId();
        if (request instanceof ComicPatchRequest pr) return pr.authorId();
        return null;
    }

    private Long extractPublisherId(Object request) {
        if (request instanceof ComicRequest cr) return cr.publisherId();
        if (request instanceof ComicPatchRequest pr) return pr.publisherId();
        return null;
    }

    private Set<Long> extractGenreIds(Object request) {
        if (request instanceof ComicRequest cr) return cr.genreIds();
        if (request instanceof ComicPatchRequest pr) return pr.genreIds();
        return Collections.emptySet();
    }

    private Optional<Author> validateAuthor(Long authorId, Map<String, String> errors) {
        if (authorId == null) return Optional.empty();
        if (!authorRepository.existsById(authorId)) {
            errors.put("authorId", String.format(AUTHOR_NOT_FOUND_MSG, authorId));
            return Optional.empty();
        }
        return Optional.of(authorRepository.getReferenceById(authorId));
    }

    private Optional<Publisher> validatePublisher(Long publisherId, Map<String, String> errors) {
        if (publisherId == null) return Optional.empty();
        if (!publisherRepository.existsById(publisherId)) {
            errors.put("publisherId", String.format(PUBLISHER_NOT_FOUND_MSG, publisherId));
            return Optional.empty();
        }
        return Optional.of(publisherRepository.getReferenceById(publisherId));
    }

    private Optional<Set<Genre>> validateGenres(Set<Long> genreIds,
                                                Map<String, String> errors) {
        if (genreIds == null || genreIds.isEmpty()) return Optional.empty();

        List<Genre> foundGenres = genreRepository.findAllById(genreIds);

        if (foundGenres.size() != genreIds.size()) {
            Set<Long> foundIds = foundGenres.stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
            List<Long> missingIds = genreIds.stream()
                .filter(gId -> !foundIds.contains(gId))
                .toList();
            errors.put("genreIds", String.format(GENRE_NOT_FOUND_MSG, missingIds));
            return Optional.empty();
        }

        return Optional.of(new HashSet<>(foundGenres));
    }

    private record ResolvedEntities(
        Optional<Author> author,
        Optional<Publisher> publisher,
        Optional<Set<Genre>> genres) {
    }
}