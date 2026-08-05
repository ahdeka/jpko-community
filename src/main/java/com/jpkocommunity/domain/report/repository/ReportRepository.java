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

    // 관리자 집계 화면용.
    // excludeAdminTargets=true면 대상 작성자(USER 신고는 대상 본인, POST/COMMENT 신고는 글쓴이)가
    // ADMIN 권한인 신고를 아예 결과에서 뺀다 — 관리자에 대한 신고는 ROOT만 볼 수 있어야 하기 때문
    // (경매로 획득한 임시 ADMIN 포함, 동료 ADMIN이 서로의 신고를 감출 수 없게 하려는 목적).
    // reports는 target_type에 따라 target_id가 posts/comments/users 중 다른 테이블을 가리키는
    // 폴리모픽 구조라, 타입별로 조건부 LEFT JOIN을 걸고 COALESCE로 하나의 role 값을 얻는다.
    @Query(
            value = """
                SELECT r.target_type AS targetType, r.target_id AS targetId,
                       COUNT(*) AS reportCount, MAX(r.created_at) AS lastReportedAt,
                       CASE MAX(CASE r.status WHEN 'PENDING' THEN 2 WHEN 'RESOLVED' THEN 1 ELSE 0 END)
                            WHEN 2 THEN 'PENDING' WHEN 1 THEN 'RESOLVED' ELSE 'REJECTED' END AS status
                FROM reports r
                LEFT JOIN users u_target ON r.target_type = 'USER' AND u_target.id = r.target_id
                LEFT JOIN posts p_target ON r.target_type = 'POST' AND p_target.id = r.target_id
                LEFT JOIN users u_post_author ON p_target.user_id = u_post_author.id
                LEFT JOIN comments c_target ON r.target_type = 'COMMENT' AND c_target.id = r.target_id
                LEFT JOIN users u_comment_author ON c_target.user_id = u_comment_author.id
                WHERE (:targetType IS NULL OR r.target_type = :targetType)
                  AND (:status IS NULL OR r.status = :status)
                  AND (:excludeAdminTargets = false OR
                       COALESCE(u_target.role, u_post_author.role, u_comment_author.role) IS DISTINCT FROM 'ADMIN')
                GROUP BY r.target_type, r.target_id
                ORDER BY MAX(r.created_at) DESC
                """,
            countQuery = """
                SELECT COUNT(*) FROM (
                    SELECT r.target_type, r.target_id FROM reports r
                    LEFT JOIN users u_target ON r.target_type = 'USER' AND u_target.id = r.target_id
                    LEFT JOIN posts p_target ON r.target_type = 'POST' AND p_target.id = r.target_id
                    LEFT JOIN users u_post_author ON p_target.user_id = u_post_author.id
                    LEFT JOIN comments c_target ON r.target_type = 'COMMENT' AND c_target.id = r.target_id
                    LEFT JOIN users u_comment_author ON c_target.user_id = u_comment_author.id
                    WHERE (:targetType IS NULL OR r.target_type = :targetType)
                      AND (:status IS NULL OR r.status = :status)
                      AND (:excludeAdminTargets = false OR
                           COALESCE(u_target.role, u_post_author.role, u_comment_author.role) IS DISTINCT FROM 'ADMIN')
                    GROUP BY r.target_type, r.target_id
                ) t
                """,
            nativeQuery = true
    )
    Page<ReportTargetSummaryRow> summarizeByTarget(
            @Param("targetType") String targetType,
            @Param("status") String status,
            @Param("excludeAdminTargets") boolean excludeAdminTargets,
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
        String getStatus();
    }

}
