package com.jpkocommunity.domain.point.controller;

import com.jpkocommunity.domain.point.dto.request.TransferRequest;
import com.jpkocommunity.domain.point.dto.response.PointResponse;
import com.jpkocommunity.domain.point.dto.response.TransferResponse;
import com.jpkocommunity.domain.point.service.PointService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody TransferRequest transferRequest
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pointService.transferPoints(authUser.userId(), transferRequest)));
    }

    @GetMapping("/hehe")
    public ResponseEntity<ApiResponse<Void>> get() {
        pointService.veryImportant();
        return ResponseEntity.ok(ApiResponse.ok("GOOOOOOD"));
    }

}