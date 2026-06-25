package com.jpkocommunity.domain.notice.controller;

import com.jpkocommunity.domain.notice.dto.request.NoticeCreateRequest;
import com.jpkocommunity.domain.notice.dto.request.NoticeUpdateRequest;
import com.jpkocommunity.domain.notice.dto.response.NoticeDetailResponse;
import com.jpkocommunity.domain.notice.dto.response.NoticeResponse;
import com.jpkocommunity.domain.notice.dto.response.NoticeSummaryResponse;
import com.jpkocommunity.domain.notice.service.NoticeService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NoticeSummaryResponse>>> getNotices(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.getNotices(pageable)));
    }

    // 게시글 목록 상단 고정 공지
    @GetMapping("/pinned")
    public ResponseEntity<ApiResponse<List<NoticeSummaryResponse>>> getPinnedNotices() {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.getPinnedNotices()));
    }

    // 메인 상단 중요 공지
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<NoticeSummaryResponse>>> getFeaturedNotices() {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.getFeaturedNotices()));
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> getNotice(
            @PathVariable Long noticeId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.getNotice(noticeId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NoticeResponse>> createNotice(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody NoticeCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        "공지사항이 등록되었습니다.",
                        noticeService.createNotice(authUser.userId(), request)
                ));
    }

    @PutMapping("/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NoticeResponse>> updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                noticeService.updateNotice(noticeId, request)
        ));
    }

    @DeleteMapping("/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(
            @PathVariable Long noticeId
    ) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.ok("공지사항이 삭제되었습니다."));
    }

}
