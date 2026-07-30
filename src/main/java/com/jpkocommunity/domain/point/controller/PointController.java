package com.jpkocommunity.domain.point.controller;

import com.jpkocommunity.domain.point.dto.response.PointResponse;
import com.jpkocommunity.domain.point.dto.response.TransactionResponse;
import com.jpkocommunity.domain.point.service.PointService;
import com.jpkocommunity.domain.point.service.TransactionService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping
    public ResponseEntity<ApiResponse<PointResponse>> getMyPoint(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pointService.getPointByUserId(authUser.userId())));
    }

}