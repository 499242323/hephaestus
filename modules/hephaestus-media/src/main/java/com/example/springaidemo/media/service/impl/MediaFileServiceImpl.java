package com.example.springaidemo.media.service.impl;

import com.example.springaidemo.media.domain.MediaFile;
import com.example.springaidemo.media.domain.StoredMediaFile;
import com.example.springaidemo.media.repository.MediaFileRepository;
import com.example.springaidemo.media.service.MediaFileService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MediaFileServiceImpl implements MediaFileService {

    private final MediaFileRepository mediaFileRepository;

    public MediaFileServiceImpl(MediaFileRepository mediaFileRepository) {
        this.mediaFileRepository = mediaFileRepository;
    }

    @Override
    public MediaFile save(String conversationId, StoredMediaFile storedFile, String sourceType, String accessUrl) {
        MediaFile saved = mediaFileRepository.save(new MediaFile(
                null,
                conversationId,
                storedFile.originalFilename(),
                storedFile.storedFilename(),
                storedFile.contentType(),
                storedFile.fileSize(),
                storedFile.storagePath(),
                "",
                sourceType,
                LocalDateTime.now()
        ));
        mediaFileRepository.updateAccessUrl(saved.id(), accessUrl);
        return new MediaFile(
                saved.id(),
                saved.conversationId(),
                saved.originalFilename(),
                saved.storedFilename(),
                saved.contentType(),
                saved.fileSize(),
                saved.storagePath(),
                accessUrl,
                saved.sourceType(),
                saved.createdAt()
        );
    }

    @Override
    public Optional<MediaFile> findById(long id) {
        return mediaFileRepository.findById(id);
    }
}
