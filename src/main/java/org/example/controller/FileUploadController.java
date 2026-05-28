package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.example.service.FileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
public class FileUploadController {
    private final FileService fileService;

    public FileUploadController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "Upload a file", description = "Accepts a file stores it in S3.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String uploadFile(
            @Parameter(
                    description = "File to upload",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart("file") MultipartFile file) {
        try {
            String fileUrl = fileService.uploadFile(file.getBytes(), file.getOriginalFilename());
            return "File uploaded successfully: " + fileUrl;
        } catch (Exception e) {
            return "File upload failed: " + e.getMessage();
        }

    }

    @DeleteMapping("/delete")
    public String deleteFile(@RequestParam("fileId") String fileId) {
        try {
            fileService.deleteFile(fileId);
            return "File deleted successfully: " + fileId;
        } catch (Exception e) {
            return "File deletion failed: " + e.getMessage();
        }
    }

    @GetMapping("/download")
    public byte[] downloadFile(@RequestParam("fileId") String fileId) {
        try {
            return fileService.getFile(fileId);
        } catch (Exception e) {
            throw new RuntimeException("File download failed: " + e.getMessage());
        }
    }
}
