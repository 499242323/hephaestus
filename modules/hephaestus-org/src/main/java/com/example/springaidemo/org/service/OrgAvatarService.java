package com.example.springaidemo.org.service;

import com.example.springaidemo.media.config.MediaStorageProperties;
import com.example.springaidemo.media.domain.StoredMediaFile;
import com.example.springaidemo.media.exception.MediaStorageException;
import com.example.springaidemo.media.service.MediaStorageService;
import com.example.springaidemo.org.domain.OrgPersonSummary;
import com.example.springaidemo.org.entity.OrgPersonEntity;
import com.example.springaidemo.org.entity.OrgUnitEntity;
import com.example.springaidemo.org.exception.OrgValidationException;
import com.example.springaidemo.org.log.service.OperationLogRecorder;
import com.example.springaidemo.org.log.support.OperationLogRecordFactory;
import com.example.springaidemo.org.repository.OrgPersonRepository;
import com.example.springaidemo.org.repository.OrgUnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
public class OrgAvatarService {

    private static final String SOURCE_TYPE = "ORG_PERSON_AVATAR";

    private final OrgScopeService orgScopeService;
    private final OrgPersonRepository orgPersonRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MediaStorageService mediaStorageService;
    private final MediaStorageProperties mediaStorageProperties;
    private final OrgAvatarPersistenceService orgAvatarPersistenceService;
    private final OperationLogRecorder operationLogRecorder;
    private final OperationLogRecordFactory operationLogRecordFactory;

    public OrgAvatarService(OrgScopeService orgScopeService,
                            OrgPersonRepository orgPersonRepository,
                            OrgUnitRepository orgUnitRepository,
                            MediaStorageService mediaStorageService,
                            MediaStorageProperties mediaStorageProperties,
                            OrgAvatarPersistenceService orgAvatarPersistenceService,
                            OperationLogRecorder operationLogRecorder,
                            OperationLogRecordFactory operationLogRecordFactory) {
        this.orgScopeService = orgScopeService;
        this.orgPersonRepository = orgPersonRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.mediaStorageService = mediaStorageService;
        this.mediaStorageProperties = mediaStorageProperties;
        this.orgAvatarPersistenceService = orgAvatarPersistenceService;
        this.operationLogRecorder = operationLogRecorder;
        this.operationLogRecordFactory = operationLogRecordFactory;
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
        try {
            OrgAvatarPersistenceService.AvatarBindingResult result = orgAvatarPersistenceService.bindAvatar(
                    personId,
                    conversationId,
                    storedFile,
                    SOURCE_TYPE,
                    accessUrl
            );
            recordAvatarLog(currentPersonId, "upload-avatar", "上传头像", result.person(),
                    "上传人员头像：" + result.person().getPersonName(),
                    "上传人员“" + result.person().getPersonName() + "”的头像。");
            return toSummary(result.person(), unit, result.accessUrl());
        } catch (RuntimeException exception) {
            log.error("头像文件已上传但数据库绑定失败，需补偿处理，personId={}, conversationId={}, storagePath={}, sourceType={}",
                    personId, conversationId, storedFile.storagePath(), SOURCE_TYPE, exception);
            throw new MediaStorageException("头像保存失败，请稍后重试", exception);
        }
    }

    public OrgPersonSummary clearAvatar(Long currentPersonId, Long personId) {
        OrgPersonEntity person = orgScopeService.requirePersonInScope(currentPersonId, personId);
        OrgUnitEntity unit = orgUnitRepository.getById(person.getUnitId());
        OrgPersonEntity refreshed = orgAvatarPersistenceService.clearAvatar(personId);
        recordAvatarLog(currentPersonId, "clear-avatar", "清空头像", refreshed,
                "清空人员头像：" + refreshed.getPersonName(),
                "清空人员“" + refreshed.getPersonName() + "”的头像。");
        return toSummary(refreshed, unit, null);
    }

    private void recordAvatarLog(Long operatorPersonId,
                                 String actionCode,
                                 String actionName,
                                 OrgPersonEntity person,
                                 String summary,
                                 String detail) {
        operationLogRecorder.recordSuccess(operationLogRecordFactory.create(
                operatorPersonId,
                "org-person",
                "人员",
                actionCode,
                actionName,
                "人员",
                person == null ? null : person.getId(),
                person == null ? null : person.getPersonName(),
                summary,
                detail
        ));
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
                person.getEnabled(),
                List.of()
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
