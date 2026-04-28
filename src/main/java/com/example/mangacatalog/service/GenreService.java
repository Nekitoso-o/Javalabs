package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.cache.ApiCacheManager;
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

@Service
public class GenreService {

    private static final Logger LOG = LoggerFactory.getLogger(GenreService.class);
    private static final String GENRE_NOT_FOUND_MSG = "Жанр c ID %s не найден!";

    private final GenreRepository repository;
    private final GenreMapper mapper;
    private final ApiCacheManager cacheManager;

    public GenreService(GenreRepository repository, GenreMapper mapper, ApiCacheManager cacheManager)
        {this.repository = repository;
        this.mapper = mapper;
        this.cacheManager = cacheManager;
    }

    @SuppressWarnings("unchecked")
    public List<GenreDto> getAll() {
        ApiCacheKey key = new ApiCacheKey("getAllGenres");

        Object cached = cacheManager.get(key);
        if (cached != null) return (List<GenreDto>) cached;

        List<GenreDto> result = repository.findAll().stream().map(mapper::toDto).toList();
        cacheManager.put(key, result);
        return result;
    }

    public GenreDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getGenreById", id);

        Object cached = cacheManager.get(key);
        if (cached != null) return (GenreDto) cached;

        LOG.info("Запрос к БД для ID: {}", id);
        Genre genre = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(GENRE_NOT_FOUND_MSG, id)));

        GenreDto result = mapper.toDto(genre);
        cacheManager.put(key, result);
        return result;
    }

    @Transactional
    public GenreDto create(GenreRequest request) {
        Genre entity = new Genre();
        entity.setName(request.name());

        GenreDto result = mapper.toDto(repository.save(entity));
        cacheManager.invalidate();
        return result;
    }

    @Transactional
    public GenreDto update(Long id, GenreRequest request) {
        Genre existing = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(GENRE_NOT_FOUND_MSG, id)));
        existing.setName(request.name());

        GenreDto result = mapper.toDto(repository.save(existing));
        cacheManager.invalidate();
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
        cacheManager.invalidate();
    }
}