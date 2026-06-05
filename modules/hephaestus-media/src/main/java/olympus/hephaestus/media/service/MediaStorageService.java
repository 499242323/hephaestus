package olympus.hephaestus.media.service;

import olympus.hephaestus.media.domain.StoredMediaFile;
import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {

    StoredMediaFile upload(String conversationId, MultipartFile file);

    StoredMediaFile upload(String conversationId, String originalFilename, String contentType, byte[] bytes, String category);

    byte[] read(String storagePath);

    StoredMediaFile uploadImageFromDataUrl(String conversationId, String dataUrl);

    StoredMediaFile uploadImageFromUrl(String conversationId, String imageUrl);
}
