package com.example.mangacatalog.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "genres")
@AttributeOverride(name = "name", column = @Column(length = 50, nullable = false, unique = true))
public class Genre extends BaseNamedEntity {

    @ManyToMany(mappedBy = "genres", fetch = FetchType.LAZY)
    private List<Comic> comics = new ArrayList<>();

    public Genre() {
        super();
    }

    public List<Comic> getComics() {
        return comics;
    }


}