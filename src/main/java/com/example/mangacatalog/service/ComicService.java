package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ComicService {

    private static final Logger LOG = LoggerFactory.getLogger(ComicService.class);
    private final ComicRepository comicRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final ComicMapper comicMapper;
    private final Map<ApiCacheKey, Object> cache = new ConcurrentHashMap<>();

    public ComicService(ComicRepository comicRepository,
                        PublisherRepository publisherRepository,
                        AuthorRepository authorRepository,
                        GenreRepository genreRepository, ComicMapper comicMapper) {
        this.comicRepository = comicRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.comicMapper = comicMapper;
    }

    private void invalidateCache() {
        LOG.info("Инвалидация: Очистка In-Memory кеша Комиксов.");
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    public List<ComicDto> getAll() {
        ApiCacheKey key = new ApiCacheKey("getAllComics");
        if (cache.containsKey(key)) {
            LOG.info("Кэш ХИТ Комиксы: {}", key);
            return (List<ComicDto>) cache.get(key);
        }
        LOG.info("Кэш МИСС Комиксы. Запрос к БД");
        List<ComicDto> result = comicRepository.findAll().stream().map(comicMapper::toDto).toList();
        cache.put(key, result);
        return result;
    }

    public ComicDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getComicById", id);
        if (cache.containsKey(key)) {
            LOG.info("Кэш ХИТ Комиксы: {}", key);
            return (ComicDto) cache.get(key);
        }
        LOG.info("Кэш МИСС Комиксы. Запрос к БД для ID: {}", id);
        Comic comic = comicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Комикс с ID " + id + " не найден!"));
        ComicDto result = comicMapper.toDto(comic);
        cache.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ComicDto> searchByTitle(String title) {
        ApiCacheKey key = new ApiCacheKey("searchByTitle", title);
        if (cache.containsKey(key)) {
            LOG.info("Кэш ХИТ Комиксы: {}", key);
            return (List<ComicDto>) cache.get(key);
        }
        LOG.info("Кэш МИСС Комиксы. Запрос к БД");
        List<ComicDto> result = comicRepository.findByTitleContainingIgnoreCase(title)
            .stream().map(comicMapper::toDto).toList();
        cache.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ComicDto> getComicsByAuthor(Long authorId) {
        ApiCacheKey key = new ApiCacheKey("getComicsByAuthor", authorId);
        if (cache.containsKey(key)) {
            LOG.info("Кэш ХИТ Комиксы: {}", key);
            return (List<ComicDto>) cache.get(key);
        }
        LOG.info("Кэш МИСС Комиксы. Запрос к БД");
        List<ComicDto> result = comicRepository.findByAuthorId(authorId)
            .stream().map(comicMapper::toDto).toList();
        cache.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ComicDto> searchComplex(String genreName, Integer minYear, int page, int size, boolean useNative) {
        ApiCacheKey key = new ApiCacheKey("searchComplex", genreName, minYear, page, size);

        if (cache.containsKey(key)) {
            LOG.info("Кэш ХИТ Комиксы (Complex): {}", key);
            return (List<ComicDto>) cache.get(key);
        }

        LOG.info("Кэш МИСС Комиксы. Запрос к БД (useNative={})", useNative);
        Pageable pageable = PageRequest.of(page, size);
        List<ComicDto> dtoList;
        if (useNative) {
            List<ComicNativeProjection> projectionList
                = comicRepository.findByGenreAndYearNative(genreName, minYear, pageable);
            dtoList = projectionList.stream().map(this::mapProjectionToDto).toList();
        } else {
            List<Comic> comicList = comicRepository.findByGenreAndYearJpql(genreName, minYear, pageable);
            dtoList = comicList.stream().map(comicMapper::toDto).toList();
        }

        cache.put(key, dtoList);
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
            .orElseThrow(() -> new ResourceNotFoundException("Автор с ID " + request.authorId() + " не найден!"));
        comic.setAuthor(author);

        Publisher publisher = publisherRepository.findById(request.publisherId())
            .orElseThrow(() -> new ResourceNotFoundException("Издатель с ID " + request.publisherId() + " не найден!"));
        comic.setPublisher(publisher);

        if (request.genreIds() != null && !request.genreIds().isEmpty()) {
            Set<Genre> genres = request.genreIds().stream()
                .map(id -> genreRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Жанр с ID " + id + " не найден!")))
                .collect(Collectors.toSet());
            comic.setGenres(genres);
        }

        Comic savedComic = comicRepository.save(comic);
        invalidateCache();
        return comicMapper.toDto(savedComic);
    }

    @Transactional
    public ComicDto update(Long id, ComicRequest request) {
        Comic existing = comicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Комикс с ID " + id + " не найден!"));
        existing.setTitle(request.title());
        existing.setReleaseYear(request.releaseYear());

        Author author = authorRepository.findById(request.authorId())
            .orElseThrow(() -> new ResourceNotFoundException("Автор с ID " + request.authorId() + " не найден!"));
        existing.setAuthor(author);

        Publisher publisher = publisherRepository.findById(request.publisherId())
            .orElseThrow(() -> new ResourceNotFoundException("Издатель с ID " + request.publisherId() + " не найден!"));
        existing.setPublisher(publisher);

        if (request.genreIds() != null) {
            Set<Genre> genres = request.genreIds().stream()
                .map(gId -> genreRepository.findById(gId)
                    .orElseThrow(() -> new ResourceNotFoundException("Жанр с ID " + gId + " не найден!")))
                .collect(Collectors.toSet());
            existing.setGenres(genres);
        }

        Comic updatedComic = comicRepository.save(existing);
        invalidateCache();
        return comicMapper.toDto(updatedComic);
    }

    @Transactional
    public void delete(Long id) {
        if (!comicRepository.existsById(id)) {
            throw new ResourceNotFoundException("Комикс с ID " + id + " не найден!");
        }
        comicRepository.deleteById(id);
        invalidateCache();
    }

    public void saveDataWithoutTransaction(TransactionDemoDto dto) {
        Publisher pub = new Publisher();
        pub.setName(dto.getPublisherName());
        publisherRepository.save(pub);

        Comic comic = new Comic();
        comic.setTitle(dto.getComicTitle());
        comic.setPublisher(pub);

        if (dto.isThrowError()) {
            throw new IllegalStateException("Внезапная ошибка БЕЗ транзакции!");
        }

        comicRepository.save(comic);
        invalidateCache();
    }

    @Transactional
    public void saveDataWithTransaction(TransactionDemoDto dto) {
        Publisher pub = new Publisher();
        pub.setName(dto.getPublisherName());
        publisherRepository.save(pub);

        Comic comic = new Comic();
        comic.setTitle(dto.getComicTitle());
        comic.setPublisher(pub);

        if (dto.isThrowError()) {
            throw new IllegalStateException("Внезапная ошибка С транзакцией! Всё будет откатано.");
        }

        comicRepository.save(comic);
        invalidateCache();
    }


    @Transactional
    public ComicDto patch(Long id, ComicPatchRequest request) {
        Comic existing = comicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Комикс с ID " + id + " не найден!"));

        if (request.title() != null) {
            existing.setTitle(request.title());
        }

        if (request.releaseYear() != null) {
            existing.setReleaseYear(request.releaseYear());
        }

        if (request.authorId() != null) {
            Author author = authorRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Автор с ID " + request.authorId() + " не найден!"));
            existing.setAuthor(author);
        }

        if (request.publisherId() != null) {
            Publisher publisher = publisherRepository.findById(request.publisherId())
                .orElseThrow(() ->
                    new ResourceNotFoundException("Издатель с ID " + request.publisherId() + " не найден!"));
            existing.setPublisher(publisher);
        }

        if (request.genreIds() != null) {
            Set<Genre> genres = request.genreIds().stream()
                .map(gId -> genreRepository.findById(gId)
                    .orElseThrow(() -> new ResourceNotFoundException("Жанр с ID " + gId + " не найден!")))
                .collect(Collectors.toSet());
            existing.setGenres(genres);
        }

        Comic updatedComic = comicRepository.save(existing);
        invalidateCache();
        return comicMapper.toDto(updatedComic);
    }
}