package com.example.mangacatalog.mapper;

import com.example.mangacatalog.dto.PublisherDto;
import com.example.mangacatalog.entity.Publisher;
import org.springframework.stereotype.Component;

@Component
public class PublisherMapper {
    public PublisherDto toDto(Publisher entity) {
        if (entity == null) {
            return null;
        }
        return new PublisherDto(entity.getId(), entity.getName());
    }
}