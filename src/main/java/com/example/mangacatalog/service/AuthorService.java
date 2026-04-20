package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.cache.ApiCacheManager;
import com.example.mangacatalog.dto.AuthorDto;
import com.example.mangacatalog.dto.AuthorRequest;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.mapper.AuthorMapper;
import com.example.mangacatalog.repository.AuthorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthorService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorService.class);
    private static final String AUTHOR_NOT_FOUND_MSG = "Автор с ID %s не найден!";

    private final AuthorRepository repository;
    private final AuthorMapper mapper;

    // Внедряем наш новый CacheManager вместо ConcurrentHashMap
    private final ApiCacheManager cacheManager;

    public AuthorService(AuthorRepository repository, AuthorMapper mapper, ApiCacheManager cacheManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.cacheManager = cacheManager;
    }

    @SuppressWarnings("unchecked")
    public List<AuthorDto> getAll() {
        ApiCacheKey key = new ApiCacheKey("getAllAuthors");

        // Пытаемся получить из кэша. Логирование HIT/MISS теперь внутри cacheManager
        Object cachedResult = cacheManager.get(key);
        if (cachedResult != null) {
            return (List<AuthorDto>) cachedResult;
        }

        LOG.info("Запрос к БД для получения всех авторов");
        List<AuthorDto> result = repository.findAll().stream().map(mapper::toDto).toList();

        // Кладем в кэш
        cacheManager.put(key, result);
        return result;
    }

    public AuthorDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getAuthorById", id);

        Object cachedResult = cacheManager.get(key);
        if (cachedResult != null) {
            return (AuthorDto) cachedResult;
        }

        LOG.info("Запрос к БД для ID: {}", id);
        Author author = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(AUTHOR_NOT_FOUND_MSG, id)));

        AuthorDto result = mapper.toDto(author);
        cacheManager.put(key, result);
        return result;
    }

    @Transactional
    public AuthorDto create(AuthorRequest request) {
        Author entity = new Author();
        entity.setName(request.name());
        AuthorDto result = mapper.toDto(repository.save(entity));

        // Инвалидируем кэш
        cacheManager.invalidate();
        return result;
    }

    @Transactional
    public AuthorDto update(Long id, AuthorRequest request) {
        Author existing = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(AUTHOR_NOT_FOUND_MSG, id)));
        existing.setName(request.name());
        AuthorDto result = mapper.toDto(repository.save(existing));

        cacheManager.invalidate();
        return result;
    }

    @Transactional
    public void delete(Long id) {
        Author author = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(AUTHOR_NOT_FOUND_MSG, id)));
        if (author.getComics() != null) {
            for (Comic comic : author.getComics()) {
                comic.setAuthor(null);
            }
        }
        repository.delete(author);

        cacheManager.invalidate();
    }
}