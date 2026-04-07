package com.example.mangacatalog.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    private List<Comic> comics = new ArrayList<>();

    public Author() {

    }

    public Long getId() {
        return id; }
    public void setId(Long id) {
        this.id = id; }

    public String getName() {
        return name; }
    public void setName(String name) {
        this.name = name; }

    public List<Comic> getComics() {
        return comics; }
    public void setComics(List<Comic> comics) {
        this.comics = comics; }
}