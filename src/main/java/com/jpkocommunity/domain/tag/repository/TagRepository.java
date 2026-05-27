package com.jpkocommunity.domain.tag.repository;

import com.jpkocommunity.domain.tag.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    // 자동완성용 - 키워드 포함된 태그 검색
    Page<Tag> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Optional<Tag> findByName(String name);
}
