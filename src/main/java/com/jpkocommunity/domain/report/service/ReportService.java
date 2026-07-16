package com.jpkocommunity.domain.report.service;

import com.jpkocommunity.domain.comment.service.CommentService;
import com.jpkocommunity.domain.post.service.PostService;
import com.jpkocommunity.domain.report.dto.request.AdminReportTargetStatusRequest;
import com.jpkocommunity.domain.report.dto.request.ReportCreateRequest;
import com.jpkocommunity.domain.report.dto.response.AdminReportDetailResponse;
import com.jpkocommunity.domain.report.dto.response.AdminReportSummaryResponse;
import com.jpkocommunity.domain.report.dto.response.MyReportResponse;
import com.jpkocommunity.domain.report.dto.response.ReportResponse;
import com.jpkocommunity.domain.report.entity.Report;
import com.jpkocommunity.domain.report.entity.ReportReason;
import com.jpkocommunity.domain.report.entity.ReportStatus;
import com.jpkocommunity.domain.report.entity.ReportTargetType;
import com.jpkocommunity.domain.report.repository.ReportRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final int PREVIEW_LENGTH = 40;

    private final ReportRepository reportRepository;
    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    @Transactional
    public ReportResponse createReport(Long userId, ReportCreateRequest request) {
        User reporter = userService.findById(userId);
        Long authorId = resolveTargetAuthorId(request.targetType(), request.targetId());

        validateReportCreatable(userId, authorId, request);

        Report report = reportRepository.save(Report.builder()
                .reporter(reporter)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reason(request.reason())
                .detail(request.detail())
                .build());

        return ReportResponse.from(report.getId());
    }

    public Page<MyReportResponse> getMyReports(Long userId, Pageable pageable) {
        Page<Report> reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(userId, pageable);

        Map<TargetKey, TargetView> views = resolveTargetViews(
                groupTargetIds(reports.getContent(), Report::getTargetType, Report::getTargetId));

        return reports.map(report -> {
            ReportTargetType type = report.getTargetType();
            TargetView view = views.get(new TargetKey(type, report.getTargetId()));
            boolean deleted = isTargetDeleted(view);
            return new MyReportResponse(
                    report.getId(), type, report.getTargetId(),
                    deleted ? null : view.postId(),
                    deleted ? deletedPreview(type) : view.preview(),
                    deleted,
                    report.getReason(), report.getDetail(), report.getStatus(), report.getCreatedAt()
            );
        });
    }

    // ========== 관리자 기능 ==========

    public Page<AdminReportSummaryResponse> getReportSummaries(
            ReportTargetType targetType, ReportStatus status, Pageable pageable) {
        Page<ReportRepository.ReportTargetSummaryRow> rows = reportRepository.summarizeByTarget(
                targetType != null ? targetType.name() : null,
                status != null ? status.name() : null,
                pageable);

        Map<TargetKey, TargetView> views = resolveTargetViews(groupTargetIds(rows.getContent(),
                row -> ReportTargetType.valueOf(row.getTargetType()),
                ReportRepository.ReportTargetSummaryRow::getTargetId));

        return rows.map(row -> {
            ReportTargetType type = ReportTargetType.valueOf(row.getTargetType());
            TargetView view = views.get(new TargetKey(type, row.getTargetId()));
            boolean deleted = isTargetDeleted(view);
            String preview = deleted ? deletedPreview(type) : view.preview();
            String author = view != null ? view.authorNickname() : "-";
            return new AdminReportSummaryResponse(
                    type, row.getTargetId(), deleted ? null : view.postId(),
                    preview, author, deleted, ReportStatus.valueOf(row.getStatus()),
                    row.getReportCount(), row.getLastReportedAt());
        });
    }

    public List<AdminReportDetailResponse> getReportDetails(ReportTargetType targetType, Long targetId) {
        return reportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId)
                .stream()
                .map(AdminReportDetailResponse::from)
                .toList();
    }

    @Transactional
    public void updateTargetStatus(AdminReportTargetStatusRequest request) {
        if (request.status() == ReportStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        List<Report> targets = reportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                request.targetType(), request.targetId());

        targets.stream()
                .filter(r -> r.getStatus() == ReportStatus.PENDING)
                .forEach(r -> {
                    if (request.status() == ReportStatus.RESOLVED) r.resolve();
                    else r.reject();
                });
    }

    // ========== private 메서드 ==========

    // 신고 대상이 활성 상태인지 확인 후 작성자 id 반환
    private Long resolveTargetAuthorId(ReportTargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> postService.findActivePostById(targetId).getUser().getId();
            case COMMENT -> commentService.findActiveComment(targetId).getUser().getId();
        };
    }

    // 신고 대상 id를 타입별로 그룹핑
    private <T> Map<ReportTargetType, List<Long>> groupTargetIds(
            List<T> items, Function<T, ReportTargetType> typeFn, Function<T, Long> idFn) {
        return items.stream().collect(Collectors.groupingBy(
                typeFn, Collectors.mapping(idFn, Collectors.toList())));
    }

    // 타입별로 대상을 한 번에 조회해 미리보기/작성자 닉네임을 매핑
    private Map<TargetKey, TargetView> resolveTargetViews(Map<ReportTargetType, List<Long>> idsByType) {
        Map<TargetKey, TargetView> views = new HashMap<>();
        idsByType.forEach((type, ids) -> views.putAll(switch (type) {
            case POST -> postService.findAllWithUserByIdIn(ids).stream()
                    .collect(Collectors.toMap(
                            p -> new TargetKey(ReportTargetType.POST, p.getId()),
                            p -> new TargetView(p.getId(), truncate(p.getTitle()),
                                    p.getUser().getNickname(), p.isDeleted())));
            case COMMENT -> commentService.findAllWithUserByIdIn(ids).stream()
                    .collect(Collectors.toMap(
                            c -> new TargetKey(ReportTargetType.COMMENT, c.getId()),
                            c -> new TargetView(c.getPost().getId(), truncate(c.getContent()),
                                    c.getUser().getNickname(), c.isDeleted() || c.getPost().isDeleted())));
        }));
        return views;
    }

    private boolean isTargetDeleted(TargetView view) {
        return view == null || view.deleted();
    }

    private String deletedPreview(ReportTargetType type) {
        return switch (type) {
            case POST -> "삭제된 게시글입니다.";
            case COMMENT -> "삭제된 댓글입니다.";
        };
    }

    private void validateReportCreatable(Long userId, Long authorId, ReportCreateRequest request) {
        // ETC 사유인 경우, 상세 사유 입력 강제
        if (request.reason() == ReportReason.ETC &&
                (request.detail() == null || request.detail().isBlank())) {
            throw new CustomException(ErrorCode.REPORT_DETAIL_REQUIRED);
        }

        // 자기 자신 신고 금지
        if (authorId.equals(userId)) {
            throw new CustomException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
        }

        // 중복 신고 금지
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                userId, request.targetType(), request.targetId())) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_EXISTS);
        }
    }

    private String truncate(String text) {
        return text.length() > PREVIEW_LENGTH ? text.substring(0, PREVIEW_LENGTH) + "…" : text;
    }

    // 배치 조회 결과를 (타입, 대상 id)로 찾기 위한 복합 키
    private record TargetKey(ReportTargetType targetType, Long targetId) {}

    // 신고 목록/집계에 노출할 대상 미리보기 + 작성자 닉네임 (postId: 원문 게시글 이동용, 게시글 신고는 자기 자신)
    private record TargetView(Long postId, String preview, String authorNickname, boolean deleted) {}

}
