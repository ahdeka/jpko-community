package com.jpkocommunity.domain.auth.event;

import com.jpkocommunity.domain.auth.entity.VerificationTokenType;
import com.jpkocommunity.domain.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class VerificationEmailEventListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VerificationEmailEvent event) {
        if (event.type() == VerificationTokenType.EMAIL_VERIFICATION) {
            emailService.sendVerificationEmail(event.email(), event.token());
        } else {
            emailService.sendPasswordResetEmail(event.email(), event.token());
        }
    }
}