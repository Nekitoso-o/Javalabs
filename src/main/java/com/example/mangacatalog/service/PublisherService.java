package com.example.mangacatalog.service;

import com.example.mangacatalog.dto.PublisherDto;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Publisher;
import com.example.mangacatalog.mapper.PublisherMapper;
import com.example.mangacatalog.repository.PublisherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublisherService {

    private final PublisherRepository repository;
    private final PublisherMapper mapper;

    public List<PublisherDto> getAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    public PublisherDto getById(Long id) {
        Publisher publisher = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Publisher with ID " + id + " not found!"));
        return mapper.toDto(publisher);
    }

    @Transactional
    public PublisherDto create(PublisherDto dto) {
        Publisher saved = repository.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }

    @Transactional
    public PublisherDto update(Long id, PublisherDto dto) {
        Publisher existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Publisher with ID " + id + " not found!"));
        existing.setName(dto.getName());
        return mapper.toDto(repository.save(existing));
    }

    @Transactional
    public PublisherDto patch(Long id, PublisherDto dto) {
        Publisher existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Publisher with ID " + id + " not found!"));
        if (dto.getName() != null) {
            existing.setName(dto.getName());
        }
        return mapper.toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        Publisher publisher = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Publisher with ID " + id + " not found!"));

        if (publisher.getComics() != null) {
            for (Comic comic : publisher.getComics()) {
                comic.setPublisher(null);
            }
        }
        repository.delete(publisher);
    }
}