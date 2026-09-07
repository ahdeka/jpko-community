package com.jpkocommunity.domain.auth.event;

import com.jpkocommunity.domain.auth.entity.VerificationTokenType;
import com.jpkocommunity.domain.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
public class VerificationEmailEventListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(VerificationEmailEvent event) {
        switch (event.type()) {
            case PASSWORD_RESET -> emailService.sendPasswordResetEmail(event.email(), event.token());
            case PRE_SIGNUP -> emailService.sendPreSignupEmail(event.email(), event.token());
            case EMAIL_CHANGE -> emailService.sendChangeEmail(event.email(), event.token());
        }
    }
}