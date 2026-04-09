package com.codehaja.infra.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/ogg", "video/quicktime"
    );

    public String upload(MultipartFile file, String folder) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("File content type is required");
        }

        boolean isImage = ALLOWED_IMAGE_TYPES.contains(contentType);
        boolean isVideo = ALLOWED_VIDEO_TYPES.contains(contentType);

        if (!isImage && !isVideo) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }

        String extension = getExtension(file.getOriginalFilename());
        String key = folder + "/" + UUID.randomUUID() + extension;

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket.trim())
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

        String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket.trim(), region.trim(), key);
        log.info("Uploaded file to S3: {}", url);
        return url;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
