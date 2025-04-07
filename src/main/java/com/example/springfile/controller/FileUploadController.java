package com.example.springfile.controller; // Updated package declaration

import com.example.springfile.model.FileInfo; // Updated import
import com.example.springfile.repository.FileInfoRepository; // Updated import
import com.example.springfile.service.FileStorageService; // Updated import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final FileInfoRepository fileInfoRepository;

    @Autowired
    public FileUploadController(FileStorageService fileStorageService, FileInfoRepository fileInfoRepository) {
        this.fileStorageService = fileStorageService;
        this.fileInfoRepository = fileInfoRepository;
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) {
        List<FileInfo> fileInfos = fileInfoRepository.findAll();
        model.addAttribute("files", fileInfos);
        return "index";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("labels") String labelsString,
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

            String storedFilename = fileStorageService.storeFile(file);
            List<String> labels = Collections.emptyList();
            if (StringUtils.hasText(labelsString)) {
                labels = Arrays.stream(labelsString.split(","))
                               .map(String::trim)
                               .filter(StringUtils::hasText)
                               .collect(Collectors.toList());
            }

            // Check if the file's name contains invalid characters
            if (originalFilename.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFilename);
            }

            // We no longer need to calculate fileExtension here for FileInfo

            FileInfo fileInfo = new FileInfo(
                    StringUtils.cleanPath(file.getOriginalFilename()),
                    storedFilename,
                    contentType, // Store the actual determined content type
                    labels
            );
            fileInfoRepository.save(fileInfo);

            redirectAttributes.addFlashAttribute("message",
                    "You successfully uploaded '" + file.getOriginalFilename() + "' with labels: " + labels);

        } catch (Exception e) {
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
}
