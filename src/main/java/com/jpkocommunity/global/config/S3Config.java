package com.jpkocommunity.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * S3Client Bean 생성
 * - 로컬(dev)에서는 application-dev.yaml에 access-key, secret-key를 채워 정적 자격증명 사용
 * - prod에서는 기본 자격증명 체인(EC2 IAM Role 등)을 사용하도록 설정
 */
@Configuration
public class S3Config {

    @Value("${cloud.aws.region.static}")
    private String region;

    // 로컬(dev)에서만 yaml에 키를 채워 정적 자격증명 사용.
    // prod에는 이 값이 없어(빈 문자열) 기본 자격증명 체인 = EC2 IAM Role로 동작.
    @Value("${cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder().region(Region.of(region));

        // 키가 둘 다 있을 때만 정적 자격증명 주입 (dev). 없으면 기본 체인(EC2 IAM Role 등).
        if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        }

        return builder.build();
    }
}
