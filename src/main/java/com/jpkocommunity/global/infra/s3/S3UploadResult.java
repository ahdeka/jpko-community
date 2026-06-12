package com.jpkocommunity.global.infra.s3;

/**
 * S3 업로드 결과 VO.
 * s3Key  — 삭제·관리용 내부 경로 (예: posts/1/uuid.jpg)
 * imageUrl — 클라이언트에 노출하는 전체 URL
 */
public record S3UploadResult(String s3Key, String imageUrl) {}
