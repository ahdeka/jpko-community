package com.jpkocommunity.global.entity;

import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 이미지 변환 처리 때 파일을 통일된 형식으로 사용 (바이트 배열, 확장자, Content-Type 등)
 */
public record ConvertedFile(byte[] bytes, Path path, String extension) {
    public static ConvertedFile of(MultipartFile file, String extension) {
        try {
            return new ConvertedFile(file.getBytes(), null, extension);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // 이미지 변환 결과는 bytes
    public static ConvertedFile of(byte[] bytes, String extension) {
        return new ConvertedFile(bytes, null, extension);
    }

    // 동영상 변환 결과 임시 경로 (bytes[] 안씀, 힙에 안올라가도록)
    public static ConvertedFile ofFile(Path path, String extension) {
        return new ConvertedFile(null, path, extension);
    }

    public boolean isFileBased() {
        return path != null;
    }
}
