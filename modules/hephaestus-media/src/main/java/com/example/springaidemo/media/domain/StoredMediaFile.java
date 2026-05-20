package com.example.springaidemo.media.domain;

public record StoredMediaFile(
        String originalFilename,
        String storedFilename,
        String contentType,
        long fileSize,
        String storagePath,
        byte[] bytes
) {
}
