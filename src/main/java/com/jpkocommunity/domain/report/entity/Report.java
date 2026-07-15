package com.jpkocommunity.domain.report.entity;

import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "reports",
        // 한 유저가 같은 대상을 중복 신고하는 것 방지
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reports_reporter_target",
                columnNames = {"reporter_id", "target_type", "target_id"}
        )
)
public class Report extends BaseEntity {

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Builder
    public Report(User reporter, ReportTargetType targetType,
                  Long targetId, ReportReason reason, String detail) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.detail = detail;
        this.status = ReportStatus.PENDING; // 기본 상태는 PENDING
    }

    // ========== 비즈니스 메서드 ==========

    public void resolve() {
        this.status = ReportStatus.RESOLVED;
    }

    public void reject() {
        this.status = ReportStatus.REJECTED;
    }
}
