package com.example.mangacatalog.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(nullable = false)
    private Integer rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comic_id", nullable = false)
    private Comic comic;

    public Review() {
        super();
    }

    public Long getId() {
        return id; }
    public void setId(Long id) {
        this.id = id; }

    public String getText() {
        return text; }
    public void setText(String text) {
        this.text = text; }

    public Integer getRating() {
        return rating; }
    public void setRating(Integer rating) {
        this.rating = rating; }

    public Comic getComic() {
        return comic; }
    public void setComic(Comic comic) {
        this.comic = comic; }
}