package com.example.springfile.model; // Updated package declaration

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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

    @ElementCollection // Stores a collection of basic types (String)
    private List<String> labels;

    public FileInfo(String filename, String storagePath, String contentType, List<String> labels) {
        this.filename = filename;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.labels = labels;
    }
}
