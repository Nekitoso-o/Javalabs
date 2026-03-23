package com.example.mangacatalog.service;

import com.example.mangacatalog.dto.AuthorDto;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.mapper.AuthorMapper;
import com.example.mangacatalog.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorService {
    private final AuthorRepository repository;
    private final AuthorMapper mapper;

    public List<AuthorDto> getAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public AuthorDto create(AuthorDto dto) {
        Author saved = repository.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}