package com.jpkocommunity.domain.auth.event;

public record FoundAccountEmailEvent(
        String email,
        String loginId,
        String token,
        String ipAddress
) {
    public static FoundAccountEmailEvent found(String email, String loginId, String token, String ipAddress) {
        return new FoundAccountEmailEvent(email, loginId, token, ipAddress);
    }
    public static FoundAccountEmailEvent notFound(String email, String ipAddress) {
        return new FoundAccountEmailEvent(email, null, null, ipAddress);
    }
    public boolean isFound() {
        return loginId != null;
    }
}