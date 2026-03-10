package com.example.mangacatalog.repository;
import com.example.mangacatalog.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository public interface ReviewRepository extends JpaRepository<Review, Long> {}