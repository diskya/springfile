package com.example.springfile.repository; // Updated package declaration

import com.example.springfile.model.FileInfo; // Updated import for FileInfo
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Added import

@Repository
public interface FileInfoRepository extends JpaRepository<FileInfo, Long> {

    // Override findAll to eagerly fetch related entities to avoid LazyInitializationException in template
    @Query("SELECT DISTINCT fi FROM FileInfo fi " +
           "LEFT JOIN FETCH fi.category " +
           "LEFT JOIN FETCH fi.subCategory " +
           "LEFT JOIN FETCH fi.labels") // Added DISTINCT and fetches for category/subCategory
    @Override
    List<FileInfo> findAll();

    // Method to find FileInfo by its unique storage path, eagerly fetching related entities
    @Query("SELECT fi FROM FileInfo fi " +
           "LEFT JOIN FETCH fi.category " +
           "LEFT JOIN FETCH fi.subCategory " +
           "LEFT JOIN FETCH fi.labels " + // Also fetch labels if needed, though not strictly required for the message fix
           "WHERE fi.storagePath = :storagePath")
    Optional<FileInfo> findByStoragePath(String storagePath);
}
