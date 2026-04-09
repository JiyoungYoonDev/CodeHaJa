package com.codehaja.infra.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/cms/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final S3StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "content") String folder
    ) throws IOException {
        String url = storageService.upload(file, folder);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
