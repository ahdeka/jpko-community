package com.jpkocommunity.global.infra.media;


import com.jpkocommunity.global.config.properties.ImageProcessingProperties;
import com.jpkocommunity.global.entity.ConvertedFile;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 동영상 -> 웹 재생용 H.264 mp4로 변환 (외부 ffmpeg 실행)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoToMp4Converter implements MediaConverter{

    private static final int TIMEOUT_SECONDS = 120;
    private static final int MAX_HEIGHT = 720;
    private static final int CRF = 28;
    private static final int ACQUIRE_WAIT_SECONDS = 20;

    private static final Semaphore TRANSCODE_SLOT = new Semaphore(1);

    private final ImageProcessingProperties imageProcessingProperties;
    private final ExternalProcessRunner processRunner;

    @Override
    public boolean supports(String extension) {
        return "mp4".equals(extension) || "mov".equals(extension);
    }

    @Override
    public ConvertedFile convert(MultipartFile file) {
        boolean acquired = false;

        try {
            acquired = TRANSCODE_SLOT.tryAcquire(ACQUIRE_WAIT_SECONDS, TimeUnit.SECONDS);

            if (!acquired) {
                log.warn("동영상 변환 슬롯 확보 실패 - 서버 혼잡");
                throw new CustomException(ErrorCode.MEDIA_CONVERT_BUSY);
            }

            Path mp4 = processRunner.runToFile(
                    file, ".video", ".mp4",
                    (input, output) -> List.of(
                            imageProcessingProperties.ffmpegPath(),
                            "-y",
                            "-i", input.toString(),
                            // 세로 720 상한(업스케일 방지) + 짝수 보정, 가로는 -2로 자동 짝수
                            "-vf", "scale=-2:trunc(min(" + MAX_HEIGHT + "\\,ih)/2)*2",
                            "-c:v", "libx264",
                            "-preset", "ultrafast",
                            "-threads", "1",
                            "-crf", String.valueOf(CRF),
                            "-pix_fmt", "yuv420p",       // 구형 브라우저/사파리 호환
                            "-movflags", "+faststart",   // 재생 시작 빠르게
                            "-c:a", "aac", "-b:a", "128k",
                            output.toString()),
                    TIMEOUT_SECONDS);

            return ConvertedFile.ofFile(mp4, "mp4");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } finally {
            if (acquired) {
                TRANSCODE_SLOT.release();
            }
        }
    }
}
