package com.jpkocommunity.domain.tag.service;

import com.jpkocommunity.domain.tag.dto.response.TagResponse;
import com.jpkocommunity.domain.tag.entity.Tag;
import com.jpkocommunity.domain.tag.repository.TagRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private static final int TAG_AUTOCOMPLETE_LIMIT = 10;
    private static final int TAG_MIN_KEYWORD_LENGTH = 2;

    // 자동완성용 - Controller에서 호출
    public List<TagResponse> findByKeyword(String keyword) {
        // 2글자 미만이면 DB 조회 안 함
        if (keyword.length() < TAG_MIN_KEYWORD_LENGTH) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, TAG_AUTOCOMPLETE_LIMIT, Sort.by(
                Sort.Order.desc("isFixed"),
                Sort.Order.asc("name")
        ));

        return tagRepository.findByNameContainingIgnoreCase(keyword, pageable)
                .stream()
                .map(TagResponse::from)
                .toList();
    }

    // PostService에서 태그명으로 Tag 엔티티 조회 or 신규 생성 시 사용
    @Transactional
    public Tag findOrCreate(String name) {
        return tagRepository.findByName(name)
                .orElseGet(() -> tagRepository.save(
                        Tag.builder()
                                .name(name)
                                .isFixed(false) // 사용자 생성 태그는 항상 false
                                .build()
                ));
    }

    // PostService에서 태그 ID로 직접 조회할 경우 대비
    public Tag findById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.TAG_NOT_FOUND));
    }
}