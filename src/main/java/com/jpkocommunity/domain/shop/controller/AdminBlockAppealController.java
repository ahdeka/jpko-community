package com.jpkocommunity.domain.shop.controller;

import com.jpkocommunity.domain.shop.dto.response.BlockAppealResponse;
import com.jpkocommunity.domain.shop.service.ActivityBlockService;
import com.jpkocommunity.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shop/block-appeals")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ROOT')")
public class AdminBlockAppealController {

    private final ActivityBlockService activityBlockService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BlockAppealResponse>>> getAppeals(
            @RequestParam(defaultValue = "false") boolean handled,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(activityBlockService.getAppeals(handled, pageable)));
    }

    @PatchMapping("/{appealId}")
    public ResponseEntity<ApiResponse<Void>> markHandled(@PathVariable Long appealId) {
        activityBlockService.markAppealHandled(appealId);
        return ResponseEntity.ok(ApiResponse.ok("처리 완료로 표시했습니다."));
    }
}
