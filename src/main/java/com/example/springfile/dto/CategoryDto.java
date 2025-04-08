package com.example.springfile.dto;

public class CategoryDto {
    private Long id;
    private String name;

    // Constructor matching the one used in the controller
    public CategoryDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters are needed for serialization (e.g., by Jackson)
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // Optional: Add setters if needed, or use Lombok @Data
    // Optional: Add NoArgsConstructor if needed for some frameworks/libraries
}
