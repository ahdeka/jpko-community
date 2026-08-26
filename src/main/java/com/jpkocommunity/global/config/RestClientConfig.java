package com.jpkocommunity.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient restClient() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(2))
                                .build());

        factory.setReadTimeout(Duration.ofSeconds(2));
        return RestClient.builder().requestFactory(factory).build();
    }
}
