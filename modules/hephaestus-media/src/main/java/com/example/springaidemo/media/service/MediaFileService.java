package com.example.springaidemo.media.service;

import com.example.springaidemo.media.domain.MediaFile;
import com.example.springaidemo.media.domain.StoredMediaFile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MediaFileService {

    MediaFile save(String conversationId, StoredMediaFile storedFile, String sourceType, String accessUrl);

    Optional<MediaFile> findById(long id);

    List<MediaFile> findByIds(Collection<Long> ids);
}
