package com.jpkocommunity.global.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class KstDateUtils {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 오늘(KST 기준) 00:00:00
    public LocalDateTime startOfToday() {
        return LocalDate.now(KST).atStartOfDay();
    }
}
