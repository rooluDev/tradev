package com.tradev.domain.item.controller;

import com.tradev.common.response.ApiResponse;
import com.tradev.infra.s3.S3Service;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items/images")
@RequiredArgsConstructor
public class ItemImageController {

    private final S3Service s3Service;

    @PostMapping("/presigned")
    public ResponseEntity<ApiResponse<List<S3Service.PresignedUrlResult>>> getPresignedUrls(
            @Valid @RequestBody PresignedUrlsRequest request) {

        List<S3Service.PresignedUrlResult> results = s3Service.generatePresignedUrls(
                request.files().stream()
                        .map(f -> new S3Service.PresignedUrlRequest(f.contentType(), f.fileSize()))
                        .toList()
        );
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    public record PresignedUrlsRequest(
            @Size(min = 1, max = 10, message = "이미지는 1~10개까지 업로드 가능합니다.")
            List<FileInfo> files
    ) {}

    public record FileInfo(String contentType, long fileSize) {}
}
