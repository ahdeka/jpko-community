package com.jpkocommunity.domain.category.entity;

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
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    // URL에 사용되는 식별자 (예: "employment", "working-holiday")
    // GET /api/posts?category=employment 처럼 쿼리 파라미터로 활용
    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    // 네비게이션 노출 순서 (숫자가 낮을수록 앞에 표시)
    @Column(nullable = false)
    private Integer displayOrder;

    @Builder
    public Category(String name, String slug, Integer displayOrder) {
        this.name = name;
        this.slug = slug;
        this.displayOrder = displayOrder;
    }
}