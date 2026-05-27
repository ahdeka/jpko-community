package com.jpkocommunity.domain.tag.controller;

import com.jpkocommunity.domain.tag.dto.response.TagResponse;
import com.jpkocommunity.domain.tag.service.TagService;
import com.jpkocommunity.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TagResponse>>> searchTags(
            @RequestParam(required = false, defaultValue = "") String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tagService.findByKeyword(keyword)));
    }
}