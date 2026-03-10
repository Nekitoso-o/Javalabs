package com.example.mangacatalog.repository;
import com.example.mangacatalog.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository public interface PublisherRepository extends JpaRepository<Publisher, Long> {}
