package com.example.mangacatalog.dto;

public interface ComicNativeProjection {
    Long getId();
    String getTitle();
    Integer getReleaseYear();

    Long getAuthorId();
    String getAuthorName();

    Long getPublisherId();
    String getPublisherName();

    String getGenreIds();
    String getGenreNames();
}