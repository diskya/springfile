package com.example.springfile.model; // Updated package declaration

import jakarta.persistence.*; // Import all persistence annotations
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime; // Import LocalDateTime
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String storagePath;
    private String contentType;
    private LocalDateTime uploadTime; // Add uploadTime field

    @ElementCollection // Stores a collection of basic types (String)
    private List<String> labels;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id") // Optional: Define the foreign key column name
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id") // Optional: Define the foreign key column name
    private SubCategory subCategory;

    // Constructor updated to include uploadTime, category, and subCategory
    public FileInfo(String filename, String storagePath, String contentType, List<String> labels, LocalDateTime uploadTime, Category category, SubCategory subCategory) {
        this.filename = filename;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.labels = labels;
        this.uploadTime = uploadTime; // Set uploadTime
        this.category = category;
        this.subCategory = subCategory;
    }
}
