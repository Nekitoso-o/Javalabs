package com.example.mangacatalog.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "comics")
public class Comic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private String genre;
    private Integer releaseYear;
}