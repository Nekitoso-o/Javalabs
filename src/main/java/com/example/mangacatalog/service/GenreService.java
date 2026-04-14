package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.dto.GenreDto;
import com.example.mangacatalog.dto.GenreRequest;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Genre;
import com.example.mangacatalog.mapper.GenreMapper;
import com.example.mangacatalog.repository.GenreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GenreService {

    private static final Logger LOG = LoggerFactory.getLogger(GenreService.class);

    private static final String GENRE_NOT_FOUND_MSG = "Жанр с ID %s не найден!";
    private static final String CACHE_HIT_MSG = "Кэш ХИТ Жанры: {}";
    private static final String CACHE_MISS_MSG = "Кэш МИСС Жанры. Запрос к БД";

    private final GenreRepository repository;
    private final GenreMapper mapper;
    private final Map<ApiCacheKey, Object> cache = new ConcurrentHashMap<>();

    public GenreService(GenreRepository repository, GenreMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    private void invalidateCache() {
        LOG.info("Инвалидация: Очистка In-Memory кеша Жанров.");
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    public List<GenreDto> getAll() {
        ApiCacheKey key = new ApiCacheKey("getAllGenres");
        if (cache.containsKey(key)) {
            LOG.info(CACHE_HIT_MSG, key);
            return (List<GenreDto>) cache.get(key);
        }
        LOG.info(CACHE_MISS_MSG);
        List<GenreDto> result = repository.findAll().stream().map(mapper::toDto).toList();
        cache.put(key, result);
        return result;
    }

    public GenreDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getGenreById", id);
        if (cache.containsKey(key)) {
            LOG.info(CACHE_HIT_MSG, key);
            return (GenreDto) cache.get(key);
        }
        LOG.info("Кэш МИСС Жанры. Запрос к БД для ID: {}", id);
        Genre genre = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(GENRE_NOT_FOUND_MSG, id)));
        GenreDto result = mapper.toDto(genre);
        cache.put(key, result);
        return result;
    }

    @Transactional
    public GenreDto create(GenreRequest request) {
        Genre entity = new Genre();
        entity.setName(request.name());
        GenreDto result = mapper.toDto(repository.save(entity));
        invalidateCache();
        return result;
    }

    @Transactional
    public GenreDto update(Long id, GenreRequest request) {
        Genre existing = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(GENRE_NOT_FOUND_MSG, id)));
        existing.setName(request.name());
        GenreDto result = mapper.toDto(repository.save(existing));
        invalidateCache();
        return result;
    }

    @Transactional
    public void delete(Long id) {
        Genre genre = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(GENRE_NOT_FOUND_MSG, id)));
        if (genre.getComics() != null) {
            for (Comic comic : genre.getComics()) {
                comic.getGenres().remove(genre);
            }
        }
        repository.delete(genre);
        invalidateCache();
    }
}