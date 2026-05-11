package com.example.mangacatalog.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comic_chapters")
public class ComicChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comic_id", nullable = false)
    private Comic comic;

    @Column(name = "chapter_number", nullable = false)
    private Double chapterNumber;

    @Column(name = "title")
    private String title;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL,
        orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<ChapterPage> pages = new ArrayList<>();

    public ComicChapter() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Comic getComic() { return comic; }
    public void setComic(Comic comic) { this.comic = comic; }

    public Double getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Double chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<ChapterPage> getPages() { return pages; }
    public void setPages(List<ChapterPage> pages) { this.pages = pages; }
}