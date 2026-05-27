package com.example.springaidemo.org.service;

import com.example.springaidemo.media.domain.MediaFile;
import com.example.springaidemo.media.service.MediaFileService;
import com.example.springaidemo.org.domain.OrgPersonSummary;
import com.example.springaidemo.org.dto.CreateOrgPersonRequest;
import com.example.springaidemo.org.dto.OrgScopeResponse;
import com.example.springaidemo.org.dto.UpdateOrgPersonRequest;
import com.example.springaidemo.org.entity.OrgPersonEntity;
import com.example.springaidemo.org.entity.OrgUnitEntity;
import com.example.springaidemo.org.exception.OrgValidationException;
import com.example.springaidemo.org.repository.OrgPersonRepository;
import com.example.springaidemo.org.repository.OrgUnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public OrgPersonService(OrgPersonRepository orgPersonRepository,
                            OrgUnitRepository orgUnitRepository,
                            OrgScopeService orgScopeService,
                            MediaFileService mediaFileService,
                            OrgPersistenceGuard orgPersistenceGuard) {
        this.orgPersonRepository = orgPersonRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.orgScopeService = orgScopeService;
        this.mediaFileService = mediaFileService;
        this.orgPersistenceGuard = orgPersistenceGuard;
    }

    public List<OrgPersonSummary> listPersons(Long currentPersonId, String personName, Long unitId, Boolean enabled) {
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        if (unitId != null && !scope.manageableUnitIds().contains(unitId)) {
            throw new OrgValidationException("查询部门超出当前管理范围");
        }
        Map<Long, OrgUnitEntity> unitMap = scope.manageableUnits().stream()
                .collect(Collectors.toMap(OrgUnitEntity::getId, Function.identity()));
        return orgPersonRepository.findByScope(List.copyOf(scope.manageableUnitIds()), personName, unitId, enabled).stream()
                .map(person -> toSummary(person, unitMap.get(person.getUnitId())))
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

    public OrgPersonSummary createPerson(Long currentPersonId, CreateOrgPersonRequest request) {
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
        entity.setRemark(trimToNull(request.remark()));
        entity.setEnabled(request.enabled() == null || request.enabled());
        try {
            orgPersonRepository.save(entity);
        } catch (RuntimeException exception) {
            log.error("创建人员失败，currentPersonId={}, unitId={}, personCode={}, username={}",
                    currentPersonId, request.unitId(), request.personCode(), request.username(), exception);
            orgPersistenceGuard.rethrowPersonCodeConflict(exception);
        }
        return toSummary(orgPersonRepository.getById(entity.getId()), unit);
    }

    public OrgPersonSummary updatePerson(Long currentPersonId, Long personId, UpdateOrgPersonRequest request) {
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
        current.setPassword(request.password().trim());
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
        return toSummary(orgPersonRepository.getById(personId), requireUnit(request.unitId()));
    }

    public void deletePerson(Long currentPersonId, Long personId) {
        orgScopeService.requirePersonInScope(currentPersonId, personId);
        orgPersonRepository.removeById(personId);
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
                person.getRemark(),
                person.getEnabled()
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
                || isBlank(request.username()) || isBlank(request.password()) || request.unitId() == null) {
            throw new OrgValidationException("人员编码、姓名、用户名、密码和所属部门不能为空");
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

    private String resolveAvatarAccessUrl(Long avatarMediaId) {
        if (avatarMediaId == null) {
            return null;
        }
        return mediaFileService.findById(avatarMediaId).map(MediaFile::accessUrl).orElse(null);
    }
}
