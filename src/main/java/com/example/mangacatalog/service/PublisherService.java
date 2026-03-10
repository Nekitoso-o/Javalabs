package com.example.mangacatalog.service;

import com.example.mangacatalog.dto.PublisherDto;
import com.example.mangacatalog.entity.Publisher;
import com.example.mangacatalog.mapper.PublisherMapper;
import com.example.mangacatalog.repository.PublisherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublisherService {

    private final PublisherRepository repository;
    private final PublisherMapper mapper;

    public List<PublisherDto> getAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    public PublisherDto create(PublisherDto dto) {
        Publisher saved = repository.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }
}