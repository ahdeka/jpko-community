package com.jpkocommunity.global.infra.media;

import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * 외부 CLI 도구를 이용한 미디어 변환 공통 실행기
 * - gif2webp, cewbp, ffmpeg 등
 * - 업로드 파일을 임시 파일로 저장 -> 출력 파일 bytes 반환 -> 임시 파일 정리
 */
@Slf4j
@Component
public class ExternalProcessRunner {

    // 입력은 스트리밍, 반환은 byte[] (이미지용)
    public byte[] run(MultipartFile file, String inputSuffix, String outputSuffix,
                      BiFunction<Path, Path, List<String>> commandFactory, int timeoutSeconds) {
        Path input = null, output = null;
        try {
            input = Files.createTempFile("media-in-", inputSuffix);
            output = Files.createTempFile("media-out-", outputSuffix);
            writeInput(file, input);

            runProcess(commandFactory.apply(input, output), timeoutSeconds, inputSuffix);
            return Files.readAllBytes(output);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
        }
    }

    // bytes[]로 안 읽고 임시파일 Path를 그대로 반환하는 메서드 (동영상용)
    public Path runToFile(MultipartFile file, String inputSuffix, String outputSuffix,
                          BiFunction<Path, Path, List<String>> commandFactory, int timeoutSeconds) {
        Path input = null, output = null;
        boolean success = false;
        try {
            input = Files.createTempFile("media-in-", inputSuffix);
            output = Files.createTempFile("media-out-", outputSuffix);
            writeInput(file, input);

            runProcess(commandFactory.apply(input, output), timeoutSeconds, inputSuffix);
            success = true;
            return output; // 경로만 반환
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } finally {
            // 입력된 임시 파일 정리
            deleteQuietly(input);
            if (!success) deleteQuietly(output);
        }
    }

    private void runProcess(List<String> command, int timeoutSeconds, String inputSuffix) {
        Process process = null;

        try {
            process = new ProcessBuilder(command)
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

        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } finally {
            // 타임아웃 처리 (프로세스 확실한 정리)
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
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

    // 입력 쓰기를 스트리밍으로 처리하는 공통 헬퍼 메서드
    private void writeInput(MultipartFile file, Path input) throws IOException {
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, input, StandardCopyOption.REPLACE_EXISTING);  // 8KB씩 흘려보냄 (힙에 통째로 안 올림)
        }
    }
}
