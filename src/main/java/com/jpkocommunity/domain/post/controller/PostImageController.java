package com.jpkocommunity.domain.post.controller;

import com.jpkocommunity.domain.post.dto.response.PostImageResponse;
import com.jpkocommunity.domain.post.service.PostImageService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/images")
@RequiredArgsConstructor
public class PostImageController {

    private final PostImageService postImageService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostImageResponse>> upload(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") int displayOrder
    ) {
        PostImageResponse response = postImageService.upload(authUser.userId(), postId, file, displayOrder);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("이미지가 업로드되었습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostImageResponse>>> getImages(
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(postImageService.getImages(postId)));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId,
            @PathVariable Long imageId
    ) {
        postImageService.delete(authUser.userId(), postId, imageId);
        return ResponseEntity.ok(ApiResponse.ok("이미지가 삭제되었습니다."));
    }
}