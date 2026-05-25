package com.example.springaidemo.org.service;

import com.example.springaidemo.media.domain.MediaFile;
import com.example.springaidemo.media.domain.StoredMediaFile;
import com.example.springaidemo.media.service.MediaFileService;
import com.example.springaidemo.org.entity.OrgPersonEntity;
import com.example.springaidemo.org.repository.OrgPersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgAvatarPersistenceService {

    private final MediaFileService mediaFileService;
    private final OrgPersonRepository orgPersonRepository;

    public OrgAvatarPersistenceService(MediaFileService mediaFileService, OrgPersonRepository orgPersonRepository) {
        this.mediaFileService = mediaFileService;
        this.orgPersonRepository = orgPersonRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public AvatarBindingResult bindAvatar(Long personId,
                                          String conversationId,
                                          StoredMediaFile storedFile,
                                          String sourceType,
                                          String accessUrl) {
        MediaFile mediaFile = mediaFileService.save(conversationId, storedFile, sourceType, accessUrl);
        orgPersonRepository.updateAvatarMediaId(personId, mediaFile.id());
        OrgPersonEntity refreshed = orgPersonRepository.getById(personId);
        return new AvatarBindingResult(refreshed, accessUrl);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrgPersonEntity clearAvatar(Long personId) {
        orgPersonRepository.clearAvatarMediaId(personId);
        return orgPersonRepository.getById(personId);
    }

    public record AvatarBindingResult(OrgPersonEntity person, String accessUrl) {
    }
}
