package com.example.springaidemo.org.service;

import com.example.springaidemo.media.service.MediaFileService;
import com.example.springaidemo.media.domain.MediaFile;
import com.example.springaidemo.org.domain.OrgPersonListRow;
import com.example.springaidemo.org.domain.OrgPersonSummary;
import com.example.springaidemo.org.dto.CreateOrgPersonRequest;
import com.example.springaidemo.org.dto.OrgScopeResponse;
import com.example.springaidemo.org.dto.UpdateOrgPersonRequest;
import com.example.springaidemo.org.entity.OrgPersonEntity;
import com.example.springaidemo.org.entity.OrgUnitEntity;
import com.example.springaidemo.org.exception.OrgValidationException;
import com.example.springaidemo.org.log.service.OperationLogRecorder;
import com.example.springaidemo.org.log.support.OperationLogRecordFactory;
import com.example.springaidemo.org.repository.OrgPersonRepository;
import com.example.springaidemo.org.repository.OrgUnitRepository;
import com.example.springaidemo.org.role.constant.OrgPermissionCodes;
import com.example.springaidemo.org.role.dto.OrgPersonRoleItem;
import com.example.springaidemo.org.role.service.OrgPermissionGuard;
import com.example.springaidemo.org.role.service.OrgPersonRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrgPersonService {

    private final OrgPersonRepository orgPersonRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgScopeService orgScopeService;
    private final MediaFileService mediaFileService;
    private final OrgPersistenceGuard orgPersistenceGuard;
    private final OrgPersonRoleService orgPersonRoleService;
    private final OrgPermissionGuard permissionGuard;
    private final OperationLogRecorder operationLogRecorder;
    private final OperationLogRecordFactory operationLogRecordFactory;

    public OrgPersonService(OrgPersonRepository orgPersonRepository,
                            OrgUnitRepository orgUnitRepository,
                            OrgScopeService orgScopeService,
                            MediaFileService mediaFileService,
                            OrgPersistenceGuard orgPersistenceGuard,
                            OrgPersonRoleService orgPersonRoleService,
                            OrgPermissionGuard permissionGuard,
                            OperationLogRecorder operationLogRecorder,
                            OperationLogRecordFactory operationLogRecordFactory) {
        this.orgPersonRepository = orgPersonRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.orgScopeService = orgScopeService;
        this.mediaFileService = mediaFileService;
        this.orgPersistenceGuard = orgPersistenceGuard;
        this.orgPersonRoleService = orgPersonRoleService;
        this.permissionGuard = permissionGuard;
        this.operationLogRecorder = operationLogRecorder;
        this.operationLogRecordFactory = operationLogRecordFactory;
    }

    public List<OrgPersonSummary> listPersonsFast(Long currentPersonId, String personName, Long unitId, Boolean enabled) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_PERSON_VIEW);
        List<OrgPersonListRow> persons = orgPersonRepository.findListRowsByCurrentScope(currentPersonId, personName, unitId, enabled);
        Map<Long, List<OrgPersonRoleItem>> roleMap = orgPersonRoleService.listPersonRolesForSummary(persons.stream()
                .map(OrgPersonListRow::id)
                .toList());
        return persons.stream()
                .map(person -> toSummary(person, roleMap.getOrDefault(person.id(), List.of())))
                .toList();
    }

    public OrgScopeResponse getCurrentScope(Long currentPersonId) {
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        Map<Long, OrgUnitEntity> unitMap = scope.manageableUnits().stream()
                .collect(Collectors.toMap(OrgUnitEntity::getId, Function.identity()));
        return new OrgScopeResponse(
                toSummary(scope.currentPerson(), unitMap.get(scope.currentPerson().getUnitId())),
                orgScopeService.buildTree(scope.manageableUnits())
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public OrgPersonSummary createPerson(Long currentPersonId, CreateOrgPersonRequest request) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_PERSON_CREATE);
        if (request != null && request.roleIds() != null) {
            permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_PERSON_ROLE_ASSIGN);
        }
        validateCreateRequest(request);
        orgScopeService.assertUnitInScope(currentPersonId, request.unitId());
        OrgPersonEntity duplicate = orgPersonRepository.getByPersonCode(request.personCode().trim());
        if (duplicate != null) {
            throw new OrgValidationException("人员编码已存在");
        }

        OrgUnitEntity unit = requireUnit(request.unitId());
        OrgPersonEntity entity = new OrgPersonEntity();
        entity.setPersonCode(request.personCode().trim());
        entity.setPersonName(request.personName().trim());
        entity.setUsername(request.username().trim());
        entity.setPassword(request.password().trim());
        entity.setUnitId(request.unitId());
        entity.setMobile(trimToNull(request.mobile()));
        entity.setEmail(trimToNull(request.email()));
        entity.setSourceType("ADMIN");
        entity.setRemark(trimToNull(request.remark()));
        entity.setEnabled(request.enabled() == null || request.enabled());
        try {
            orgPersonRepository.save(entity);
        } catch (RuntimeException exception) {
            log.error("创建人员失败，currentPersonId={}, unitId={}, personCode={}, username={}",
                    currentPersonId, request.unitId(), request.personCode(), request.username(), exception);
            orgPersistenceGuard.rethrowPersonCodeConflict(exception);
        }
        if (request.roleIds() != null) {
            orgPersonRoleService.replacePersonRolesForPersonSave(currentPersonId, entity.getId(), request.roleIds());
        }
        OrgPersonEntity created = orgPersonRepository.getById(entity.getId());
        recordPersonLog(currentPersonId, "create", "新增", created, unit, "新增人员：" + created.getPersonName(),
                "新增人员“" + created.getPersonName() + "”，所属部门为“" + unit.getUnitName() + "”，登录账号为“" + created.getUsername() + "”。");
        return toSummary(created, unit);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrgPersonSummary updatePerson(Long currentPersonId, Long personId, UpdateOrgPersonRequest request) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_PERSON_UPDATE);
        if (request != null && request.roleIds() != null) {
            permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_PERSON_ROLE_ASSIGN);
        }
        validateUpdateRequest(request);
        OrgPersonEntity current = orgScopeService.requirePersonInScope(currentPersonId, personId);
        orgScopeService.assertUnitInScope(currentPersonId, request.unitId());
        OrgPersonEntity duplicate = orgPersonRepository.getByPersonCode(request.personCode().trim());
        if (duplicate != null && !Objects.equals(duplicate.getId(), personId)) {
            throw new OrgValidationException("人员编码已存在");
        }

        current.setPersonCode(request.personCode().trim());
        current.setPersonName(request.personName().trim());
        current.setUsername(request.username().trim());
        if (!isBlank(request.password())) {
            current.setPassword(request.password().trim());
        }
        current.setUnitId(request.unitId());
        current.setMobile(trimToNull(request.mobile()));
        current.setEmail(trimToNull(request.email()));
        current.setRemark(trimToNull(request.remark()));
        current.setEnabled(request.enabled() == null || request.enabled());
        try {
            orgPersonRepository.update(current);
        } catch (RuntimeException exception) {
            log.error("更新人员失败，currentPersonId={}, personId={}, unitId={}, personCode={}, username={}",
                    currentPersonId, personId, request.unitId(), request.personCode(), request.username(), exception);
            orgPersistenceGuard.rethrowPersonCodeConflict(exception);
        }
        if (request.roleIds() != null) {
            orgPersonRoleService.replacePersonRolesForPersonSave(currentPersonId, personId, request.roleIds());
        }
        OrgPersonEntity updated = orgPersonRepository.getById(personId);
        OrgUnitEntity unit = requireUnit(request.unitId());
        recordPersonLog(currentPersonId, "update", "修改", updated, unit, "修改人员：" + updated.getPersonName(),
                "修改人员“" + updated.getPersonName() + "”：所属部门为“" + unit.getUnitName() + "”，手机号为“" + blankText(updated.getMobile()) + "”，邮箱为“" + blankText(updated.getEmail()) + "”。");
        return toSummary(updated, unit);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePerson(Long currentPersonId, Long personId) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_PERSON_DELETE);
        OrgPersonEntity person = orgScopeService.requirePersonInScope(currentPersonId, personId);
        OrgUnitEntity unit = requireUnit(person.getUnitId());
        orgPersonRepository.removeById(personId);
        recordPersonLog(currentPersonId, "delete", "删除", person, unit, "删除人员：" + person.getPersonName(),
                "删除人员“" + person.getPersonName() + "”，原所属部门为“" + unit.getUnitName() + "”。");
    }

    private void recordPersonLog(Long operatorPersonId,
                                 String actionCode,
                                 String actionName,
                                 OrgPersonEntity person,
                                 OrgUnitEntity unit,
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

    public OrgUnitEntity requireUnit(Long unitId) {
        OrgUnitEntity unit = orgUnitRepository.getById(unitId);
        if (unit == null) {
            throw new OrgValidationException("部门不存在");
        }
        return unit;
    }

    public OrgPersonSummary toSummary(OrgPersonEntity person, OrgUnitEntity unit) {
        String accessUrl = resolveAvatarAccessUrl(person.getAvatarMediaId());
        List<OrgPersonRoleItem> roles = person.getId() == null ? List.of() : orgPersonRoleService.listPersonRolesForSummary(person.getId());
        return toSummary(person, unit, accessUrl, roles);
    }

    private OrgPersonSummary toSummary(OrgPersonEntity person, OrgUnitEntity unit, String accessUrl, List<OrgPersonRoleItem> roles) {
        return new OrgPersonSummary(
                person.getId(),
                person.getPersonCode(),
                person.getPersonName(),
                person.getUsername(),
                person.getPassword(),
                person.getUnitId(),
                unit == null ? null : unit.getUnitName(),
                person.getAvatarMediaId(),
                accessUrl,
                person.getMobile(),
                person.getEmail(),
                person.getSourceType(),
                person.getRemark(),
                person.getEnabled(),
                roles == null ? List.of() : roles
        );
    }

    private OrgPersonSummary toSummary(OrgPersonListRow person, List<OrgPersonRoleItem> roles) {
        return new OrgPersonSummary(
                person.id(),
                person.personCode(),
                person.personName(),
                person.username(),
                person.password(),
                person.unitId(),
                person.unitName(),
                person.avatarMediaId(),
                person.avatarAccessUrl(),
                person.mobile(),
                person.email(),
                person.sourceType(),
                person.remark(),
                person.enabled(),
                roles == null ? List.of() : roles
        );
    }

    private void validateCreateRequest(CreateOrgPersonRequest request) {
        if (request == null || isBlank(request.personCode()) || isBlank(request.personName())
                || isBlank(request.username()) || isBlank(request.password()) || request.unitId() == null) {
            throw new OrgValidationException("人员编码、姓名、用户名、密码和所属部门不能为空");
        }
    }

    private void validateUpdateRequest(UpdateOrgPersonRequest request) {
        if (request == null || isBlank(request.personCode()) || isBlank(request.personName())
                || isBlank(request.username()) || request.unitId() == null) {
            throw new OrgValidationException("人员编码、姓名、用户名和所属部门不能为空");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String blankText(String value) {
        return value == null || value.isBlank() ? "空" : value;
    }

    private String resolveAvatarAccessUrl(Long avatarMediaId) {
        if (avatarMediaId == null) {
            return null;
        }
        return mediaFileService.findById(avatarMediaId).map(MediaFile::accessUrl).orElse(null);
    }
}
