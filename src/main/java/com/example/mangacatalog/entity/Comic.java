package com.example.mangacatalog.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "comics")
public class Comic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(name = "release_year", nullable = false)
    private Integer releaseYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    private Publisher publisher;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "comic_genres",
        joinColumns = @JoinColumn(name = "comic_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @OneToMany(mappedBy = "comic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();


    public Comic() {

    }


    public Long getId() {
        return id; }
    public void setId(Long id) {
        this.id = id; }

    public String getTitle() {
        return title; }
    public void setTitle(String title) {
        this.title = title; }

    public Integer getReleaseYear() {
        return releaseYear; }
    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear; }

    public Author getAuthor() {
        return author; }
    public void setAuthor(Author author) {
        this.author = author; }

    public Publisher getPublisher() {
        return publisher; }
    public void setPublisher(Publisher publisher) {
        this.publisher = publisher; }

    public Set<Genre> getGenres() {
        return genres; }
    public void setGenres(Set<Genre> genres) {
        this.genres = genres; }

    public List<Review> getReviews() {
        return reviews; }
    public void setReviews(List<Review> reviews) {
        this.reviews = reviews; }
}