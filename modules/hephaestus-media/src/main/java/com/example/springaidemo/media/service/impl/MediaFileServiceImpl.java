package com.example.springaidemo.media.service.impl;

import com.example.springaidemo.media.domain.MediaFile;
import com.example.springaidemo.media.domain.StoredMediaFile;
import com.example.springaidemo.media.repository.MediaFileRepository;
import com.example.springaidemo.media.service.MediaFileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MediaFileServiceImpl implements MediaFileService {

    private final MediaFileRepository mediaFileRepository;

    public MediaFileServiceImpl(MediaFileRepository mediaFileRepository) {
        this.mediaFileRepository = mediaFileRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MediaFile save(String conversationId, StoredMediaFile storedFile, String sourceType, String accessUrl) {
        MediaFile saved = mediaFileRepository.saveMediaFile(new MediaFile(
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

    @Override
    public List<MediaFile> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mediaFileRepository.findByIds(ids.stream().distinct().toList());
    }
}
