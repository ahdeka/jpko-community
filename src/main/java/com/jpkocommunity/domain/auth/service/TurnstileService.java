package com.jpkocommunity.domain.auth.service;

import com.jpkocommunity.global.config.properties.TurnstileProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class TurnstileService {

    private final RestClient restClient;
    private final TurnstileProperties properties;

    public void verify(String token, String remoteIp) {
        if (!properties.enabled()) {
            return;
        }

        if (token == null || token.isBlank()) {
            throw new CustomException(ErrorCode.CAPTCHA_FAILED);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", properties.secretKey());
        form.add("response", token);
        if (remoteIp != null) {
            form.add("remoteip", remoteIp);
        }

        TurnstileResponse result;

        try {
            result = restClient.post()
                    .uri(properties.verifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TurnstileResponse.class);
        } catch (RestClientException e) {
            // Cloudflare 응답 지연 -> 인증 실패로 처리
            throw new CustomException(ErrorCode.CAPTCHA_FAILED);
        }

        if (result == null || !result.success()) {
            throw new CustomException(ErrorCode.CAPTCHA_FAILED);
        }
    }

    private record TurnstileResponse(boolean success) {
    }

}
