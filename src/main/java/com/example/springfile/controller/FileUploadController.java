package com.example.springfile.controller; // Updated package declaration

import com.example.springfile.model.Category; // Added import
import com.example.springfile.model.FileInfo;
import com.example.springfile.model.SubCategory; // Added import
import com.example.springfile.repository.CategoryRepository; // Added import
import com.example.springfile.repository.FileInfoRepository;
import com.example.springfile.repository.SubCategoryRepository; // Added import
import com.example.springfile.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
// Using Jackson for robust JSON conversion
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ContentDisposition; // Added import
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder; // Added import
import java.nio.charset.StandardCharsets; // Added import
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap; // Added import for HashMap
import java.util.List;
import java.util.Map; // Added import for Map
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger; // Added import for Logger
import org.slf4j.LoggerFactory; // Added import for LoggerFactory
import com.example.springfile.dto.CategoryDto; // Added import for external DTO
import com.example.springfile.dto.SubCategoryDto; // Added import for external DTO

@Controller
public class FileUploadController {

    // Inner DTO classes removed, using external ones now.

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class); // Added logger

    private final FileStorageService fileStorageService;
    private final FileInfoRepository fileInfoRepository;
    private final CategoryRepository categoryRepository; // Added repository
    private final SubCategoryRepository subCategoryRepository; // Added repository
    private final ObjectMapper objectMapper; // Jackson ObjectMapper for JSON

    @Autowired
    public FileUploadController(FileStorageService fileStorageService,
                                FileInfoRepository fileInfoRepository,
                                CategoryRepository categoryRepository, // Added repository
                                SubCategoryRepository subCategoryRepository, // Added repository
                                ObjectMapper objectMapper) { // Inject ObjectMapper
        this.fileStorageService = fileStorageService;
        this.fileInfoRepository = fileInfoRepository;
        this.categoryRepository = categoryRepository; // Added repository
        this.subCategoryRepository = subCategoryRepository; // Added repository
        this.objectMapper = objectMapper; // Assign ObjectMapper
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) {
        List<FileInfo> fileInfos = fileInfoRepository.findAll();
        log.info("Fetched {} FileInfo records.", fileInfos.size()); // Log file info count

        List<Category> categories = categoryRepository.findAll(); // Fetch all categories
        log.info("Fetched {} Category records.", categories.size()); // Log category count
        if (!categories.isEmpty()) {
            log.info("Categories fetched: {}", categories.stream().map(Category::getName).collect(Collectors.toList())); // Log category names
        }

        // Fetch all subcategories - consider performance for large datasets
        List<SubCategory> subCategories = subCategoryRepository.findAll();
        log.info("Fetched {} SubCategory records.", subCategories.size()); // Log subcategory count

        // --- Added Logging for Orphaned Subcategories ---
        for (SubCategory sc : subCategories) {
            if (sc.getCategory() == null || sc.getCategory().getId() == null) {
                log.warn("SubCategory found with missing Category association: ID={}, Name='{}'", sc.getId(), sc.getName());
            }
        }
        // ------------------------------------------------

        // Subcategory grouping will be handled in JavaScript

        Map<Long, String> fileExtensions = new HashMap<>();

        for (FileInfo fileInfo : fileInfos) {
            String filename = fileInfo.getFilename();
            String extension = "N/A";
            if (filename != null && !filename.isEmpty() && filename.contains(".")) {
                extension = filename.substring(filename.lastIndexOf(".") + 1);
            }
            // Use fileInfo.getId() which should be non-null for persisted entities
            if (fileInfo.getId() != null) {
                 fileExtensions.put(fileInfo.getId(), extension);
            } else {
                // Log a warning if ID is null, though this shouldn't happen for findAll() results
                log.warn("FileInfo found with null ID: {}", fileInfo);
            }
        }

        model.addAttribute("files", fileInfos);
        model.addAttribute("fileExtensions", fileExtensions);
        // model.addAttribute("categories", categories); // Remove this line - use JSON DTOs instead

        // Map entities to DTOs before serialization
        List<CategoryDto> categoryDtos = categories.stream()
                .map(cat -> new CategoryDto(cat.getId(), cat.getName()))
                .collect(Collectors.toList());

        List<SubCategoryDto> subCategoryDtos = subCategories.stream()
                // Ensure category and its ID are not null before creating DTO
                .filter(sc -> sc.getCategory() != null && sc.getCategory().getId() != null)
                .map(sc -> new SubCategoryDto(sc.getId(), sc.getName(), sc.getCategory().getId()))
                .collect(Collectors.toList());

        // Pass DTO lists as JSON strings for client-side handling
        String categoriesJson = convertListToJson(categoryDtos, "categories");
        String subCategoriesJson = convertListToJson(subCategoryDtos, "subCategories");
        model.addAttribute("categoriesJson", categoriesJson);
        model.addAttribute("subCategoriesJson", subCategoriesJson);
        log.info("Passing categoriesJson (DTOs) to template: {}", categoriesJson);
        log.info("Passing subCategoriesJson (DTOs) to template: {}", subCategoriesJson);


        return "index";
    }

    // Generic helper method to convert List to JSON String using Jackson
    private String convertListToJson(List<?> list, String listName) {
        try {
            // Important: Need to handle potential circular references (e.g., SubCategory -> Category -> SubCategories)
            // This might require @JsonIgnore or custom serializers on the model classes.
            // Let's assume for now Jackson handles it or we'll address it if an error occurs.
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Error converting {} list to JSON", listName, e);
            return "[]"; // Return empty JSON array on error
        }
    }


    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("labels") String labelsString,
                                   @RequestParam("category") String categoryValue, // Renamed from categoryName to reflect it can be ID or "new"
                                   @RequestParam(name = "newCategory", required = false) String newCategoryName, // Capture new category name
                                   @RequestParam("subCategory") String subCategoryValue, // Renamed from subCategoryName
                                   @RequestParam(name = "newSubCategory", required = false) String newSubCategoryName, // Capture new sub-category name
                                   RedirectAttributes redirectAttributes) {

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload.");
            return "redirect:/";
        }

        try {
            // Determine content type from the uploaded file itself
            String contentType = file.getContentType();
            // Fallback if content type is not provided by the browser/client
            if (!StringUtils.hasText(contentType)) {
                contentType = "application/octet-stream"; // Or try to guess based on extension if needed
            }

            // 1. Store the physical file
            String storedFilename = fileStorageService.storeFile(file);

            // 2. Prepare labels
            List<String> labels = Collections.emptyList();
            if (StringUtils.hasText(labelsString)) {
                labels = Arrays.stream(labelsString.split(","))
                               .map(String::trim)
                               .filter(StringUtils::hasText)
                               .collect(Collectors.toList());
            }

            // 3. Save metadata (including category/subcategory handling)
            fileStorageService.saveFileMetadata(
                    originalFilename,
                    storedFilename,
                    contentType,
                    labels,
                    categoryValue,      // Pass the value (ID or "new")
                    newCategoryName,    // Pass the potential new name
                    subCategoryValue,   // Pass the value (ID or "new")
                    newSubCategoryName  // Pass the potential new name
            );

            // Fetch the saved FileInfo to get actual category/subcategory names (This part remains correct)
            Optional<FileInfo> savedFileInfoOpt = fileInfoRepository.findByStoragePath(storedFilename);
            String finalCategoryName = "N/A";
            String finalSubCategoryName = "N/A";

            if (savedFileInfoOpt.isPresent()) {
                FileInfo savedFileInfo = savedFileInfoOpt.get();
                if (savedFileInfo.getCategory() != null) {
                    finalCategoryName = savedFileInfo.getCategory().getName();
                }
                if (savedFileInfo.getSubCategory() != null) {
                    finalSubCategoryName = savedFileInfo.getSubCategory().getName();
                }
            } else {
                log.warn("Could not find saved FileInfo immediately after saving for storedFilename: {}", storedFilename);
                // Fallback logic (less likely needed now, but keep for safety)
                // If fetch fails, we don't have the final names easily available here anymore.
                // The message might be less informative in this rare failure case.
                log.warn("Could not retrieve saved FileInfo, success message might be incomplete.");
                finalCategoryName = "[Category Info Unavailable]";
                finalSubCategoryName = "[SubCategory Info Unavailable]";
            }
            // Construct the success message using the retrieved names
            String successMessage = String.format(
                "You successfully uploaded '%s' with Category: %s, SubCategory: %s, Labels: %s",
                originalFilename,
                finalCategoryName,
                finalSubCategoryName,
                labels.isEmpty() ? "None" : String.join(", ", labels) // Improved label display
            );
            redirectAttributes.addFlashAttribute("message", successMessage);


        } catch (Exception e) {
             log.error("Error during file upload for {}", originalFilename, e); // Log the exception
            redirectAttributes.addFlashAttribute("message",
                    "Could not upload file: " + file.getOriginalFilename() + ". Error: " + e.getMessage());
        }

        return "redirect:/";
    }

    // Endpoint for downloading files
    @GetMapping("/files/{storedFilename:.+}") // Use .+ to capture filenames with dots
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String storedFilename, HttpServletRequest request) { // HttpServletRequest might not be needed anymore
        Resource resource = fileStorageService.loadFileAsResource(storedFilename);

        // Find the FileInfo from the database efficiently
        Optional<FileInfo> fileInfoOpt = fileInfoRepository.findByStoragePath(storedFilename);

        // Determine filename and content type from stored FileInfo
        String downloadFilename = fileInfoOpt.map(FileInfo::getFilename).orElse(storedFilename); // Fallback to stored name if not found
        String contentType = fileInfoOpt.map(FileInfo::getContentType).orElse("application/octet-stream"); // Use stored type, fallback if missing



        // Manually encode filename using UTF-8 for broader compatibility
        String encodedFilename = URLEncoder.encode(downloadFilename, StandardCharsets.UTF_8).replace("+", "%20");

        // Build ContentDisposition header with the encoded filename
        // Use filename* for RFC 5987 encoding, though manual encoding is often more reliable across servers
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(encodedFilename) // Use the encoded filename
                .build();


        // Alternative using filename* (less commonly supported but standard)
        // ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
        //         .filename(downloadFilename, StandardCharsets.UTF_8)
        //         .build();


        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

    // Endpoint for deleting files using storedFilename
    @PostMapping("/files/delete/{storedFilename:.+}") // Use storedFilename in the path
    public String deleteFile(@PathVariable String storedFilename, RedirectAttributes redirectAttributes) { // Use storedFilename as parameter
        Optional<FileInfo> fileInfoOpt = fileInfoRepository.findByStoragePath(storedFilename); // Find by storage path
        if (fileInfoOpt.isPresent()) {
            FileInfo fileInfo = fileInfoOpt.get();
            try {
                fileStorageService.deleteFile(fileInfo.getStoragePath()); // Delete physical file using the path from FileInfo
                fileInfoRepository.delete(fileInfo); // Delete database record using the entity
                redirectAttributes.addFlashAttribute("message", "Successfully deleted file: " + fileInfo.getFilename());
                log.info("Deleted file: {} (Stored: {})", fileInfo.getFilename(), storedFilename); // Log deletion
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("message", "Could not delete file: " + fileInfo.getFilename() + ". Error: " + e.getMessage());
                log.error("Error deleting file: {} (Stored: {})", fileInfo.getFilename(), storedFilename, e); // Log error
            }
        } else {
            redirectAttributes.addFlashAttribute("message", "File not found for deletion: " + storedFilename);
            log.warn("Attempted to delete non-existent file with storedFilename: {}", storedFilename); // Log warning
        }
        return "redirect:/";
    }
}
