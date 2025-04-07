package com.example.springfile.repository;

import com.example.springfile.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    // Add custom query methods if needed, e.g., findByNameIgnoreCase
    Optional<Category> findByNameIgnoreCase(String name);
}
