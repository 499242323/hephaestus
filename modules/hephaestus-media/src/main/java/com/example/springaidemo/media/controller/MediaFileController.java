package com.example.springaidemo.media.controller;

import com.example.springaidemo.media.domain.MediaFile;
import com.example.springaidemo.media.service.MediaFileService;
import com.example.springaidemo.media.service.MediaStorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/media/files")
public class MediaFileController {

    private final MediaFileService mediaFileService;
    private final MediaStorageService mediaStorageService;

    public MediaFileController(MediaFileService mediaFileService, MediaStorageService mediaStorageService) {
        this.mediaFileService = mediaFileService;
        this.mediaStorageService = mediaStorageService;
    }

    @GetMapping("/{id}")
    public void view(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
        writeFile(id, false, response);
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
        writeFile(id, true, response);
    }

    private void writeFile(long id, boolean attachment, HttpServletResponse response) throws IOException {
        MediaFile mediaFile = mediaFileService.findById(id)
                .orElseThrow(() -> new MediaFileNotFoundException("文件不存在"));
        byte[] bytes = mediaStorageService.read(mediaFile.storagePath());

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(mediaFile.contentType());
        response.setContentLength(bytes.length);
        ContentDisposition contentDisposition = (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(mediaFile.originalFilename(), StandardCharsets.UTF_8)
                .build();
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        response.getOutputStream().write(bytes);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static class MediaFileNotFoundException extends RuntimeException {
        private MediaFileNotFoundException(String message) {
            super(message);
        }
    }
}
