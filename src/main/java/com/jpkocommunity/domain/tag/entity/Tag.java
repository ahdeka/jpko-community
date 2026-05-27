package com.jpkocommunity.domain.tag.entity;

import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
@Table(name = "tags")
public class Tag extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    // true: 운영자가 만든 고정 태그 → 사용자가 삭제 불가
    // false: 사용자가 게시글 작성 시 생성한 태그
    @Column(nullable = false)
    private boolean isFixed;

    @Builder
    public Tag(String name, boolean isFixed) {
        this.name = name;
        this.isFixed = isFixed;
    }
}