package com.example.springfile.service; // Updated package declaration

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import com.example.springfile.model.Category;
import com.example.springfile.model.FileInfo;
import com.example.springfile.model.SubCategory;
import com.example.springfile.repository.CategoryRepository;
import com.example.springfile.repository.FileInfoRepository;
import com.example.springfile.repository.SubCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime; // Import LocalDateTime
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List; // Import List
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path storageLocation;

    private final FileInfoRepository fileInfoRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    @Autowired // Constructor injection
    public FileStorageService(FileInfoRepository fileInfoRepository, CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository) {
        this.fileInfoRepository = fileInfoRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    @PostConstruct
    public void init() {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

     /**
     * Stores the physical uploaded file and returns the unique filename (storage path) used for storage.
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
     * Saves the metadata for a file after it has been physically stored.
     * Handles finding or creating categories and subcategories.
     *
     * @param originalFilename The original name of the uploaded file.
     * @param storagePath      The unique path/filename where the file is stored (returned by storeFile).
     * @param contentType        The MIME type of the file.
     * @param labels             The list of labels associated with the file.
     * @param categoryValue      The value from the category dropdown (ID or "new").
     * @param newCategoryName    The name entered if categoryValue is "new".
     * @param subCategoryValue   The value from the subcategory dropdown (ID, "new", or empty).
     * @param newSubCategoryName The name entered if subCategoryValue is "new".
     * @return The saved FileInfo entity.
     */
    public FileInfo saveFileMetadata(String originalFilename, String storagePath, String contentType, List<String> labels,
                                     String categoryValue, String newCategoryName,
                                     String subCategoryValue, String newSubCategoryName) {

        Category category = null;
        SubCategory subCategory = null;

        // 1. Determine Category
        if ("new".equals(categoryValue)) {
            if (StringUtils.hasText(newCategoryName)) {
                // Find existing or create new if "new" is selected and name is provided
                category = categoryRepository.findByName(newCategoryName)
                        .orElseGet(() -> categoryRepository.save(new Category(newCategoryName.trim())));
            } else {
                // Handle error: "new" selected but no name provided (should ideally be caught by frontend validation)
                throw new IllegalArgumentException("New category name cannot be empty when 'Add New' is selected.");
            }
        } else if (StringUtils.hasText(categoryValue)) {
            // Find existing category by ID if a value other than "new" is provided
            try {
                Long categoryId = Long.parseLong(categoryValue);
                category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid Category ID: " + categoryId));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid Category value: " + categoryValue);
            }
        } else {
             // Handle error: No category selected (should be caught by frontend validation)
             throw new IllegalArgumentException("Category must be selected.");
         }

        // Make category effectively final for use in lambdas below
        final Category finalCategory = category;

        // 2. Determine SubCategory (only if a valid Category was determined)
        if (finalCategory != null) {
            if ("new".equals(subCategoryValue)) {
                if (StringUtils.hasText(newSubCategoryName)) {
                    // Find existing or create new subcategory within the determined category
                    subCategory = subCategoryRepository.findByNameAndCategory(newSubCategoryName.trim(), finalCategory)
                            .orElseGet(() -> subCategoryRepository.save(new SubCategory(newSubCategoryName.trim(), finalCategory)));
                } else {
                    // Handle error: "new" selected but no name provided
                     throw new IllegalArgumentException("New sub-category name cannot be empty when 'Add New' is selected.");
                }
            } else if (StringUtils.hasText(subCategoryValue)) {
                // Find existing subcategory by ID if a value other than "new" or empty is provided
                 try {
                    Long subCategoryId = Long.parseLong(subCategoryValue);
                    // Important: Ensure the found subcategory belongs to the selected category (optional check)
                    subCategory = subCategoryRepository.findById(subCategoryId)
                            .filter(sc -> sc.getCategory().getId().equals(finalCategory.getId())) // Use finalCategory here
                            .orElseThrow(() -> new IllegalArgumentException("Invalid SubCategory ID: " + subCategoryId + " for Category: " + finalCategory.getName())); // Use finalCategory here
                 } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid SubCategory value: " + subCategoryValue);
                 }
            }
            // If subCategoryValue is empty or null, subCategory remains null (optional subcategory)
        }

        // 3. Create and save FileInfo
        FileInfo fileInfo = new FileInfo(
                originalFilename,
                storagePath, // Use the unique storage path
                contentType,
                labels,
                LocalDateTime.now(),
                finalCategory, // Use the effectively final variable
                subCategory // Can be null
        );
        return fileInfoRepository.save(fileInfo);
    }


    /**
     * Loads a file as a Resource using its stored unique filename.
     * @param storedFilename The unique filename (storagePath in FileInfo).
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

    /**
     * Deletes a file from the storage location.
     * @param storedFilename The unique filename used when storing the file.
     * @throws IOException If an I/O error occurs during deletion.
     */
    public void deleteFile(String storedFilename) throws IOException {
        try {
            Path filePath = this.storageLocation.resolve(storedFilename).normalize();
            // Use deleteIfExists to avoid exception if file is already gone
            boolean deleted = Files.deleteIfExists(filePath);
            if (!deleted) {
                // Optionally log or handle the case where the file didn't exist
                // For now, we just proceed as the goal is for the file to be gone
                System.out.println("Attempted to delete file that did not exist: " + storedFilename); // Simple logging
            }
        } catch (MalformedURLException ex) {
            // This shouldn't happen if storedFilename is valid, but handle defensively
            throw new RuntimeException("Invalid file path format for deletion: " + storedFilename, ex);
        } catch (IOException ex) {
            // Rethrow IOExceptions to be handled by the controller
            throw new IOException("Could not delete file " + storedFilename, ex);
        }
    }
}
