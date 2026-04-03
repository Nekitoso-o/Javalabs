package com.example.mangacatalog.service;

import com.example.mangacatalog.dto.AuthorDto;
import com.example.mangacatalog.entity.Author;
import com.example.mangacatalog.entity.Comic;
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

    public AuthorDto getById(Long id) {
        Author author = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Author with ID " + id + " not found!"));
        return mapper.toDto(author);
    }

    @Transactional
    public AuthorDto create(AuthorDto dto) {
        Author saved = repository.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }

    @Transactional
    public AuthorDto update(Long id, AuthorDto dto) {
        Author existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Author with ID " + id + " not found!"));
        existing.setName(dto.getName());
        return mapper.toDto(repository.save(existing));
    }

    @Transactional
    public AuthorDto patch(Long id, AuthorDto dto) {
        Author existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Author with ID " + id + " not found!"));
        if (dto.getName() != null) {
            existing.setName(dto.getName());
        }
        return mapper.toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        Author author = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Author with ID " + id + " not found!"));

        if (author.getComics() != null) {
            for (Comic comic : author.getComics()) {
                comic.setAuthor(null);
            }
        }
        repository.delete(author);
    }
}