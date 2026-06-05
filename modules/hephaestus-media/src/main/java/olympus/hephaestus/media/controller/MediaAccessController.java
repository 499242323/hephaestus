package olympus.hephaestus.media.controller;

import olympus.hephaestus.media.service.MediaStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/mediadl/media/getdata")
public class MediaAccessController {

    private static final String ACCESS_PREFIX = "/mediadl/media/getdata/";

    private final MediaStorageService mediaStorageService;

    public MediaAccessController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @GetMapping("/**")
    public void view(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String storagePath = extractStoragePath(request);
//        log.info("访问媒体展示接口: storagePath={}", storagePath);
        byte[] bytes = mediaStorageService.read(storagePath);

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(detectContentType(storagePath));
        response.setContentLength(bytes.length);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");
        response.getOutputStream().write(bytes);
    }

    private String extractStoragePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int index = uri.indexOf(ACCESS_PREFIX);
        if (index < 0) {
            throw new MediaAccessException("媒体访问路径不正确");
        }
        String storagePath = uri.substring(index + ACCESS_PREFIX.length());
        if (storagePath.isBlank()) {
            throw new MediaAccessException("媒体存储路径不能为空");
        }
        return storagePath;
    }

    private String detectContentType(String storagePath) {
        return MediaTypeFactory.getMediaType(storagePath)
                .orElse(MediaType.APPLICATION_OCTET_STREAM)
                .toString();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static class MediaAccessException extends RuntimeException {
        private MediaAccessException(String message) {
            super(message);
        }
    }
}
