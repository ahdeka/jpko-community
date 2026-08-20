package com.jpkocommunity.global.infra.media;

import com.jpkocommunity.global.config.properties.ImageProcessingProperties;
import com.jpkocommunity.global.entity.ConvertedFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * GIF -> WebP 변환기 (외부 gif2webp 실행)
 */
@Component
@RequiredArgsConstructor
public class Gif2WebpConverter implements MediaConverter {

    private static final int TIMEOUT_SECONDS = 15;      // 프로세스 hang 방지
    private static final int QUALITY = 50;              // 품질 수치
    private final ImageProcessingProperties imageProcessingProperties;
    private final ExternalProcessRunner processRunner;

    @Override
    public boolean supports(String extension) {
        return "gif".equals(extension);
    }

    @Override
    public ConvertedFile convert(MultipartFile file) {
        byte[] webp = processRunner.run(
                file, ".gif", ".webp",
                (input, output) -> List.of(
                        imageProcessingProperties.gif2webpPath(),
                        "-lossy", "-q", String.valueOf(QUALITY),
                        input.toString(), "-o", output.toString()),
                TIMEOUT_SECONDS);

        return ConvertedFile.of(webp, "webp");
    }

}
