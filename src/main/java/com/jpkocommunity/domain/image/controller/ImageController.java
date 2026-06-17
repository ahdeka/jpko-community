package com.jpkocommunity.domain.image.controller;

import com.jpkocommunity.domain.image.dto.ImageUploadResponse;
import com.jpkocommunity.domain.image.service.ImageService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    // TipTap 에디터 이미지 즉시 업로드 용
    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ImageUploadResponse>> upload(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam("image") MultipartFile image
            ) {
        return ResponseEntity.ok(ApiResponse.ok(imageService.uploadTemp(image)));
    }
}
