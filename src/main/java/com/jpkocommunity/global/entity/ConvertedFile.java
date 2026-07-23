package com.jpkocommunity.global.entity;

import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 이미지 변환 처리 때 파일을 통일된 형식으로 사용 (바이트 배열, 확장자, Content-Type 등)
 */
public record ConvertedFile(byte[] bytes, String extension, String contentType) {
    public static ConvertedFile of(MultipartFile file, String extension) {
        try {
            return new ConvertedFile(file.getBytes(), extension,
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public static ConvertedFile of(byte[] bytes, String extension, String contentType) {
        return new ConvertedFile(bytes, extension, contentType);
    }
}
