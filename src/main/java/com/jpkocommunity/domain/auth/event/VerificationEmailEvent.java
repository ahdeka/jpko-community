package com.jpkocommunity.domain.auth.event;

import com.jpkocommunity.domain.auth.entity.VerificationTokenType;

public record VerificationEmailEvent(
        String email,
        String token,
        VerificationTokenType type)
{}