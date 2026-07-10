package com.jpkocommunity.domain.user.event;

import com.jpkocommunity.domain.auth.repository.VerificationTokenRepository;
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
public class UserWithdrawnEventListener {

    private final VerificationTokenRepository verificationTokenRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(UserWithdrawnEvent event) {
        long deleted = verificationTokenRepository.deleteByUserId(event.userId());
        log.info("[회원 탈퇴 후처리] userId={}, 잔여 verification_tokens {}건 정리", event.userId(), deleted);
    }
}
