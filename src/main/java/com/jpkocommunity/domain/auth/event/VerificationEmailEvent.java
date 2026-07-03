package com.jpkocommunity.domain.auth.entity;

public record VerificationEmailEvent(
        String email,
        String token,
        VerificationTokenType type)
{}