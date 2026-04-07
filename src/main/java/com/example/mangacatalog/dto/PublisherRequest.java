package com.example.mangacatalog.dto;

import jakarta.validation.constraints.NotBlank;

public record PublisherRequest(
    @NotBlank(message = "Название издателя обязательно")
    String name
) {

}