package com.jpkocommunity.domain.tag.service;

import com.jpkocommunity.domain.tag.dto.response.TagResponse;
import com.jpkocommunity.domain.tag.entity.Tag;
import com.jpkocommunity.domain.tag.repository.TagRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final int TAG_AUTOCOMPLETE_LIMIT = 10;
    private static final int TAG_MIN_KEYWORD_LENGTH = 2;

    private final TagRepository tagRepository;

    // 자동완성용 - Controller에서 호출
    public List<TagResponse> findByKeyword(String keyword) {
        // 공백 제거 후 길이 체크
        String normalized = keyword.strip();

        // 2글자 미만이면 DB 조회 안 함
        if (normalized.length() < TAG_MIN_KEYWORD_LENGTH) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, TAG_AUTOCOMPLETE_LIMIT, Sort.by(
                Sort.Order.desc("fixed"),
                Sort.Order.asc("name")
        ));

        return tagRepository.findByNameContainingIgnoreCase(normalized, pageable)
                .stream()
                .map(TagResponse::from)
                .toList();
    }

    // PostService에서 태그명으로 Tag 엔티티 조회 or 신규 생성 시 사용
    @Transactional
    public Tag findOrCreate(String name) {
        String normalized = name.strip().toLowerCase();

        return tagRepository.findByName(normalized)
                .orElseGet(() -> {
                    try {
                        return tagRepository.save(
                                Tag.builder()
                                        .name(normalized)
                                        .fixed(false)
                                        .build()
                        );
                    } catch (DataIntegrityViolationException e) {
                        // 동시 요청으로 unique constraint 위반 시
                        return tagRepository.findByName(normalized)
                                .orElseThrow();
                    }
                });
    }

    // PostService에서 태그 ID로 직접 조회할 경우 대비
    public Tag findById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.TAG_NOT_FOUND));
    }
}