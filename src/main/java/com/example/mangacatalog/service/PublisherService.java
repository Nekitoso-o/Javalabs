package com.example.mangacatalog.service;

import com.example.mangacatalog.cache.ApiCacheKey;
import com.example.mangacatalog.cache.ApiCacheManager;
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

@Service
public class PublisherService {

    private static final Logger LOG = LoggerFactory.getLogger(PublisherService.class);
    private static final String PUBLISHER_NOT_FOUND_MSG = "Издатель с ID %s не найден!";

    private final PublisherRepository repository;
    private final PublisherMapper mapper;
    private final ApiCacheManager cacheManager;

    public PublisherService(PublisherRepository repository, PublisherMapper mapper, ApiCacheManager cacheManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.cacheManager = cacheManager;
    }

    @SuppressWarnings("unchecked")
    public List<PublisherDto> getAll() {
        ApiCacheKey key = new ApiCacheKey("getAllPublishers");

        Object cached = cacheManager.get(key);
        if (cached != null) return (List<PublisherDto>) cached;

        List<PublisherDto> result = repository.findAll().stream().map(mapper::toDto).toList();
        cacheManager.put(key, result);
        return result;
    }

    public PublisherDto getById(Long id) {
        ApiCacheKey key = new ApiCacheKey("getPublisherById", id);

        Object cached = cacheManager.get(key);
        if (cached != null) return (PublisherDto) cached;

        LOG.info("Запрос к БД для ID: {}", id);
        Publisher publisher = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(PUBLISHER_NOT_FOUND_MSG, id)));

        PublisherDto result = mapper.toDto(publisher);
        cacheManager.put(key, result);
        return result;
    }

    @Transactional
    public PublisherDto create(PublisherRequest request) {
        Publisher entity = new Publisher();
        entity.setName(request.name());

        PublisherDto result = mapper.toDto(repository.save(entity));
        cacheManager.invalidate();
        return result;
    }

    @Transactional
    public PublisherDto update(Long id, PublisherRequest request) {
        Publisher existing = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(PUBLISHER_NOT_FOUND_MSG, id)));
        existing.setName(request.name());

        PublisherDto result = mapper.toDto(repository.save(existing));
        cacheManager.invalidate();
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
        cacheManager.invalidate();
    }
}