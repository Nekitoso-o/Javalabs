package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.dto.AuthorDto;
import com.example.mangacatalog.dto.AuthorRequest;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.mapper.AuthorMapper;
import com.example.mangacatalog.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorService {
    private final AuthorRepository repository;
    private final AuthorMapper mapper;

    private final Map<ApiCacheKey, Object> cache = new ConcurrentHashMap<>();

    private void invalidateCache() {
        log.info("♻ Инвалидация: Очистка In-Memory кеша Авторов.");
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    public List<AuthorDto> getAll() {
        ApiCacheKey key = new ApiCacheKey("getAllAuthors");
        if (cache.containsKey(key)) {
            log.info("Кэш ХИТ Авторы: {}", key);
            return (List<AuthorDto>) cache.get(key);
        }
        log.info("Кэш МИСС Авторы. Запрос к БД");
        List<AuthorDto> result = repository.findAll().stream().map(mapper::toDto).toList();
        cache.put(key, result);
        return result;
    }

    public AuthorDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getAuthorById", id);
        if (cache.containsKey(key)) {
            log.info("Кэш ХИТ Авторы: {}", key);
            return (AuthorDto) cache.get(key);
        }
        log.info("Кэш МИСС Авторы. Запрос к БД для ID: {}", id);
        Author author = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Автор с ID " + id + " не найден!"));
        AuthorDto result = mapper.toDto(author);
        cache.put(key, result);
        return result;
    }

    @Transactional
    public AuthorDto create(AuthorRequest request) {
        Author entity = new Author();
        entity.setName(request.name());
        AuthorDto result = mapper.toDto(repository.save(entity));
        invalidateCache();
        return result;
    }

    @Transactional
    public AuthorDto update(Long id, AuthorRequest request) {
        Author existing = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Автор с ID " + id + " не найден!"));
        existing.setName(request.name());
        AuthorDto result = mapper.toDto(repository.save(existing));
        invalidateCache();
        return result;
    }

    @Transactional
    public void delete(Long id) {
        Author author = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Автор с ID " + id + " не найден!"));

        if (author.getComics() != null) {
            for (Comic comic : author.getComics()) {
                comic.setAuthor(null);
            }
        }
        repository.delete(author);
        invalidateCache();
    }
}