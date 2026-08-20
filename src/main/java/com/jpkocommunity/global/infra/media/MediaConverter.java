package com.jpkocommunity.global.infra.media;

import com.jpkocommunity.global.entity.ConvertedFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 확장자별 미디어 변환기
 * 지원하는 확장자면 변환 결과(ConvertedFile)를 돌려주기 위함
 */
public interface MediaConverter {

    boolean supports(String extension);

    ConvertedFile convert(MultipartFile file);
}
