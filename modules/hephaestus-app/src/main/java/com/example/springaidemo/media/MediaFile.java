package com.example.springaidemo.media;

import java.time.LocalDateTime;

public record MediaFile(
        Long id,
        String conversationId,
        String originalFilename,
        String storedFilename,
        String contentType,
        long fileSize,
        String storagePath,
        String accessUrl,
        String sourceType,
        LocalDateTime createdAt
) {
}
