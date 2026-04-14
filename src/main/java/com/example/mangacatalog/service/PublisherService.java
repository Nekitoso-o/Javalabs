package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.dto.PublisherDto;
import com.example.mangacatalog.dto.PublisherRequest;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Publisher;
import com.example.mangacatalog.mapper.PublisherMapper;
import com.example.mangacatalog.repository.PublisherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PublisherService {

    private static final Logger LOG = LoggerFactory.getLogger(PublisherService.class);

    private static final String PUBLISHER_NOT_FOUND_MSG = "Издатель с ID %s не найден!";
    private static final String CACHE_HIT_MSG = "Кэш ХИТ Издатели: {}";
    private static final String CACHE_MISS_MSG = "Кэш МИСС Издатели. Запрос к БД";

    private final PublisherRepository repository;
    private final PublisherMapper mapper;
    private final Map<ApiCacheKey, Object> cache = new ConcurrentHashMap<>();

    public PublisherService(PublisherRepository repository, PublisherMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    private void invalidateCache() {
        LOG.info("Инвалидация: Очистка In-Memory кеша Издателей.");
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    public List<PublisherDto> getAll() {
        ApiCacheKey key = new ApiCacheKey("getAllPublishers");
        if (cache.containsKey(key)) {
            LOG.info(CACHE_HIT_MSG, key);
            return (List<PublisherDto>) cache.get(key);
        }
        LOG.info(CACHE_MISS_MSG);
        List<PublisherDto> result = repository.findAll().stream().map(mapper::toDto).toList();
        cache.put(key, result);
        return result;
    }

    public PublisherDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getPublisherById", id);
        if (cache.containsKey(key)) {
            LOG.info(CACHE_HIT_MSG, key);
            return (PublisherDto) cache.get(key);
        }
        LOG.info("Кэш МИСС Издатели. Запрос к БД для ID: {}", id);
        Publisher publisher = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(PUBLISHER_NOT_FOUND_MSG, id)));
        PublisherDto result = mapper.toDto(publisher);
        cache.put(key, result);
        return result;
    }

    @Transactional
    public PublisherDto create(PublisherRequest request) {
        Publisher entity = new Publisher();
        entity.setName(request.name());
        PublisherDto result = mapper.toDto(repository.save(entity));
        invalidateCache();
        return result;
    }

    @Transactional
    public PublisherDto update(Long id, PublisherRequest request) {
        Publisher existing = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(PUBLISHER_NOT_FOUND_MSG, id)));
        existing.setName(request.name());
        PublisherDto result = mapper.toDto(repository.save(existing));
        invalidateCache();
        return result;
    }

    @Transactional
    public void delete(Long id) {
        Publisher publisher = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(PUBLISHER_NOT_FOUND_MSG, id)));

        if (publisher.getComics() != null) {
            for (Comic comic : publisher.getComics()) {
                comic.setPublisher(null);
            }
        }
        repository.delete(publisher);
        invalidateCache();
    }
}