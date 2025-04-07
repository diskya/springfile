package com.example.springfile.service; // Updated package declaration

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}") // Inject upload directory path from properties
    private String uploadDir;

    private Path storageLocation;

    @PostConstruct // Executed after dependency injection is done
    public void init() {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    /**
     * Stores the uploaded file and returns the unique filename used for storage.
     * @param file The uploaded file.
     * @return The unique filename generated for storage.
     */
    public String storeFile(MultipartFile file) {
        // Normalize file name
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        try {
            // Check if the file's name contains invalid characters
            if (originalFilename.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFilename);
            }

            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) { // Use >= 0 to handle files starting with '.'
                fileExtension = originalFilename.substring(dotIndex);
            }
            // Generate a unique filename to avoid collisions
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.storageLocation.resolve(uniqueFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            return uniqueFilename; // Return the unique name used for storage
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFilename + ". Please try again!", ex);
        }
    }

    /**
     * Loads a file as a Resource.
     * @param storedFilename The unique filename used when storing the file.
     * @return The file as a Resource.
     */
    public Resource loadFileAsResource(String storedFilename) {
        try {
            Path filePath = this.storageLocation.resolve(storedFilename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + storedFilename);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + storedFilename, ex);
        }
    }
}
