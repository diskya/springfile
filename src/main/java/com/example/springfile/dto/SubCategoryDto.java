package com.example.springfile.dto;

public class SubCategoryDto {
    private Long id;
    private String name;
    private Long categoryId; // Include category ID for JS filtering

    // Constructor matching the one used in the controller
    public SubCategoryDto(Long id, String name, Long categoryId) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
    }

    // Getters are needed for serialization (e.g., by Jackson)
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    // Optional: Add setters if needed, or use Lombok @Data
    // Optional: Add NoArgsConstructor if needed for some frameworks/libraries
}
