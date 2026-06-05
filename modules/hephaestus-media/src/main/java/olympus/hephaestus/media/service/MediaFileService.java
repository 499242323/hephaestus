package olympus.hephaestus.media.service;

import olympus.hephaestus.media.domain.MediaFile;
import olympus.hephaestus.media.domain.StoredMediaFile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MediaFileService {

    MediaFile save(String conversationId, StoredMediaFile storedFile, String sourceType, String accessUrl);

    Optional<MediaFile> findById(long id);

    List<MediaFile> findByIds(Collection<Long> ids);
}
