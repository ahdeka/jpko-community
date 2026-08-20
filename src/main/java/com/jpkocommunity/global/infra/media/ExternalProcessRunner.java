package com.jpkocommunity.global.infra.media;

import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * 외부 CLI 도구를 이용한 미디어 변환 공통 실행기
 *   - gif2webp, cewbp, ffmpeg 등
 *   - 업로드 파일을 임시 파일로 저장 -> 출력 파일 bytes 반환 -> 임시 파일 정리
 */
@Slf4j
@Component
public class ExternalProcessRunner {

    public byte[] run(MultipartFile file, String inputSuffix, String outputSuffix,
                      BiFunction<Path, Path, List<String>> commandFactory, int timeoutSeconds) {
        Path input = null, output = null;

        try {
            input = Files.createTempFile("media-in-", inputSuffix);
            output = Files.createTempFile("media-out-", outputSuffix);
            Files.write(input, file.getBytes());

            Process process = new ProcessBuilder(commandFactory.apply(input, output))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.warn("외부 프로세스 타임아웃 - 변환 거부 (input: {})", inputSuffix);
                throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
            }
            if (process.exitValue() != 0) {
                log.warn("외부 프로세스 변환 실패 - exitCode: {} (input: {})", process.exitValue(), inputSuffix);
                throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
            }

            return Files.readAllBytes(output);

        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("임시 파일 삭제 실패: {}", path);
        }
    }
}
