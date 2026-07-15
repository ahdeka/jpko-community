package com.jpkocommunity.domain.report.event;

import com.jpkocommunity.domain.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportEventListener {

    private final ReportRepository reportRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(ContentDeletedEvent event) {
        int resolved = reportRepository.resolveAllPendingByTarget(event.targetType(), event.targetId());
        if (resolved > 0) {
            log.info("[신고 자동 처리] targetType={}, targetId={}, {}건 RESOLVED 전환",
                    event.targetType(), event.targetId(), resolved);
        }
    }
}