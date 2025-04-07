package com.example.springfile.repository;

import com.example.springfile.model.SubCategory;
import com.example.springfile.model.Category; // Import Category
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
    Optional<SubCategory> findByNameAndCategory(String name, Category category);
    List<SubCategory> findByCategory(Category category);
    // Add other custom query methods if needed
}
