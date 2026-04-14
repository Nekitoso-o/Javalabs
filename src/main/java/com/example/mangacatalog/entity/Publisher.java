package com.example.mangacatalog.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publishers")
@AttributeOverride(name = "name", column = @Column(length = 100, nullable = false))
public class Publisher extends BaseNamedEntity {

    @OneToMany(mappedBy = "publisher", cascade = CascadeType.ALL)
    private List<Comic> comics = new ArrayList<>();

    public Publisher() {
        super();
    }

    public List<Comic> getComics() {
        return comics;
    }


}