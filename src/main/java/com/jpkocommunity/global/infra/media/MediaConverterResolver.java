package com.jpkocommunity.global.infra.media;

import com.jpkocommunity.global.entity.ConvertedFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MediaConverterResolver {

    private final List<MediaConverter> converters;

    public ConvertedFile convert(MultipartFile file, String extension) {
        return converters.stream()
                .filter(converter -> converter.supports(extension))
                .findFirst()
                .map(converter -> converter.convert(file))
                .orElseGet(() -> ConvertedFile.of(file, extension));

    }
}
