package com.example.springfile.repository; // Updated package declaration

import com.example.springfile.model.FileInfo; // Updated import for FileInfo
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Added import

@Repository
public interface FileInfoRepository extends JpaRepository<FileInfo, Long> {

    // Override findAll to eagerly fetch labels to avoid LazyInitializationException in template
    @Query("SELECT fi FROM FileInfo fi LEFT JOIN FETCH fi.labels")
    @Override
    List<FileInfo> findAll();

    // Method to find FileInfo by its unique storage path
    Optional<FileInfo> findByStoragePath(String storagePath);
}
