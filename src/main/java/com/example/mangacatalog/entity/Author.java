package com.example.mangacatalog.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "authors")
@AttributeOverride(name = "name", column = @Column(length = 100, nullable = false))
public class Author extends BaseNamedEntity {

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    private List<Comic> comics = new ArrayList<>();

    public Author() {
        super();
    }

    public List<Comic> getComics() {
        return comics;
    }

}