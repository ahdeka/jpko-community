package com.jpkocommunity.domain.checkin.controller;

import com.jpkocommunity.domain.checkin.dto.response.CheckInCalendarResponse;
import com.jpkocommunity.domain.checkin.dto.response.CheckInOutcome;
import com.jpkocommunity.domain.checkin.service.CheckInService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/check-ins")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping
    public ResponseEntity<ApiResponse<CheckInOutcome>> checkIn(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("출석이 완료되었습니다.",
                checkInService.checkIn(authUser.userId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CheckInCalendarResponse>> getMyCheckIns(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                checkInService.getMonthlyCheckIns(authUser.userId(), year, month)
        ));
    }
}
