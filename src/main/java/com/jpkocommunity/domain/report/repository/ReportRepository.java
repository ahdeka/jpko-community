package com.jpkocommunity.domain.report.repository;

import com.jpkocommunity.domain.report.entity.Report;
import com.jpkocommunity.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(
            Long reporterId, ReportTargetType targetType, Long targetId);

    // 특정 대상에 대한 신고 전체 조회 (관리자용)
    List<Report> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            ReportTargetType targetType, Long targetId
    );

    // 마이페이지 "내 신고 내역"
    Page<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);

    // 관리자 집계 화면용
    @Query(
            value = """
                SELECT target_type AS targetType, target_id AS targetId,
                       COUNT(*) AS reportCount, MAX(created_at) AS lastReportedAt
                FROM reports
                WHERE (:targetType IS NULL OR target_type = :targetType)
                  AND (:status IS NULL OR status = :status)
                GROUP BY target_type, target_id
                ORDER BY MAX(created_at) DESC
                """,
            countQuery = """
                SELECT COUNT(*) FROM (
                    SELECT target_type, target_id FROM reports
                    WHERE (:targetType IS NULL OR target_type = :targetType)
                      AND (:status IS NULL OR status = :status)
                    GROUP BY target_type, target_id
                ) t
                """,
            nativeQuery = true
    )
    Page<ReportTargetSummaryRow> summarizeByTarget(
            @Param("targetType") String targetType,
            @Param("status") String status,
            Pageable pageable
    );

    // 컨텐츠 삭제 이벤트로 인한 일괄 처리
    @Modifying
    @Query("UPDATE Report r SET r.status = com.jpkocommunity.domain.report.entity.ReportStatus.RESOLVED " +
            "WHERE r.targetType = :targetType AND r.targetId = :targetId " +
            "AND r.status = com.jpkocommunity.domain.report.entity.ReportStatus.PENDING")
    int resolveAllPendingByTarget(
            @Param("targetType") ReportTargetType targetType, @Param("targetId") Long targetId);

    // 네이티브 쿼리 결과를 매핑할 프로젝션
    interface ReportTargetSummaryRow {
        String getTargetType();
        Long getTargetId();
        Long getReportCount();
        LocalDateTime getLastReportedAt();
    }

}
