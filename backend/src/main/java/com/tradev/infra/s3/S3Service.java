package com.tradev.infra.s3;

import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${aws.s3.bucket}")
    private String bucket;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public List<PresignedUrlResult> generatePresignedUrls(List<PresignedUrlRequest> requests) {
        return requests.stream()
                .map(this::generatePresignedUrl)
                .toList();
    }

    public PresignedUrlResult generatePresignedUrl(PresignedUrlRequest request) {
        if (!ALLOWED_CONTENT_TYPES.contains(request.contentType())) {
            throw new TradevException(ErrorCode.FILE_INVALID_TYPE);
        }
        if (request.fileSize() > MAX_FILE_SIZE) {
            throw new TradevException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        String s3Key = "images/" + UUID.randomUUID() + getExtension(request.contentType());

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(request.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putRequest)
                .signatureDuration(Duration.ofMinutes(10))
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        return new PresignedUrlResult(presigned.url().toString(), s3Key);
    }

    public void deleteFile(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete S3 file: {}", s3Key, e);
        }
    }

    private String getExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }

    public record PresignedUrlRequest(String contentType, long fileSize) {}
    public record PresignedUrlResult(String uploadUrl, String s3Key) {}
}
