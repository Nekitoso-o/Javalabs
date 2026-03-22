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
        PublisherDto dto = new PublisherDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        return dto;
    }

    public Publisher toEntity(PublisherDto dto) {
        if (dto == null) {
            return null;
        }
        Publisher entity = new Publisher();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        return entity;
    }
}