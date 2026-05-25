package com.example.springaidemo.org.service;

import com.example.springaidemo.media.config.MediaStorageProperties;
import com.example.springaidemo.media.domain.StoredMediaFile;
import com.example.springaidemo.media.service.MediaStorageService;
import com.example.springaidemo.org.domain.OrgPersonSummary;
import com.example.springaidemo.org.entity.OrgPersonEntity;
import com.example.springaidemo.org.entity.OrgUnitEntity;
import com.example.springaidemo.org.exception.OrgValidationException;
import com.example.springaidemo.org.repository.OrgPersonRepository;
import com.example.springaidemo.org.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OrgAvatarService {

    private static final String SOURCE_TYPE = "ORG_PERSON_AVATAR";

    private final OrgScopeService orgScopeService;
    private final OrgPersonRepository orgPersonRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MediaStorageService mediaStorageService;
    private final MediaStorageProperties mediaStorageProperties;
    private final OrgAvatarPersistenceService orgAvatarPersistenceService;

    public OrgAvatarService(OrgScopeService orgScopeService,
                            OrgPersonRepository orgPersonRepository,
                            OrgUnitRepository orgUnitRepository,
                            MediaStorageService mediaStorageService,
                            MediaStorageProperties mediaStorageProperties,
                            OrgAvatarPersistenceService orgAvatarPersistenceService) {
        this.orgScopeService = orgScopeService;
        this.orgPersonRepository = orgPersonRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.mediaStorageService = mediaStorageService;
        this.mediaStorageProperties = mediaStorageProperties;
        this.orgAvatarPersistenceService = orgAvatarPersistenceService;
    }

    public OrgPersonSummary bindAvatar(Long currentPersonId, Long personId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new OrgValidationException("头像文件不能为空");
        }
        OrgPersonEntity person = orgScopeService.requirePersonInScope(currentPersonId, personId);
        OrgUnitEntity unit = orgUnitRepository.getById(person.getUnitId());
        String conversationId = "org-person-" + personId;
        StoredMediaFile storedFile = mediaStorageService.upload(conversationId, file);
        String accessUrl = buildAccessUrl(storedFile.storagePath());
        OrgAvatarPersistenceService.AvatarBindingResult result = orgAvatarPersistenceService.bindAvatar(
                personId,
                conversationId,
                storedFile,
                SOURCE_TYPE,
                accessUrl
        );
        return toSummary(result.person(), unit, result.accessUrl());
    }

    public OrgPersonSummary clearAvatar(Long currentPersonId, Long personId) {
        OrgPersonEntity person = orgScopeService.requirePersonInScope(currentPersonId, personId);
        OrgUnitEntity unit = orgUnitRepository.getById(person.getUnitId());
        OrgPersonEntity refreshed = orgAvatarPersistenceService.clearAvatar(personId);
        return toSummary(refreshed, unit, null);
    }

    private OrgPersonSummary toSummary(OrgPersonEntity person, OrgUnitEntity unit, String avatarAccessUrl) {
        return new OrgPersonSummary(
                person.getId(),
                person.getPersonCode(),
                person.getPersonName(),
                person.getUsername(),
                person.getPassword(),
                person.getUnitId(),
                unit == null ? null : unit.getUnitName(),
                person.getAvatarMediaId(),
                avatarAccessUrl,
                person.getMobile(),
                person.getEmail(),
                person.getRemark(),
                person.getEnabled()
        );
    }

    private String buildAccessUrl(String storagePath) {
        String prefix = mediaStorageProperties.getAccessPathPrefix();
        String normalizedPrefix = prefix == null || prefix.isBlank() ? "/mediadl/media/getdata" : prefix.trim();
        while (normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }
        String normalizedPath = storagePath == null ? "" : storagePath.trim();
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return normalizedPrefix + "/" + normalizedPath;
    }
}
