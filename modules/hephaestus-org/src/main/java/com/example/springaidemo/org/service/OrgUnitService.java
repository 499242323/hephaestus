package com.example.springaidemo.org.service;

import com.example.springaidemo.org.domain.OrgUnitTreeNode;
import com.example.springaidemo.org.dto.CreateOrgUnitRequest;
import com.example.springaidemo.org.dto.UpdateOrgUnitRequest;
import com.example.springaidemo.org.entity.OrgUnitEntity;
import com.example.springaidemo.org.exception.OrgValidationException;
import com.example.springaidemo.org.repository.OrgPersonRepository;
import com.example.springaidemo.org.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class OrgUnitService {

    private final OrgUnitRepository orgUnitRepository;
    private final OrgPersonRepository orgPersonRepository;
    private final OrgScopeService orgScopeService;
    private final OrgPersistenceGuard orgPersistenceGuard;

    public OrgUnitService(OrgUnitRepository orgUnitRepository,
                          OrgPersonRepository orgPersonRepository,
                          OrgScopeService orgScopeService,
                          OrgPersistenceGuard orgPersistenceGuard) {
        this.orgUnitRepository = orgUnitRepository;
        this.orgPersonRepository = orgPersonRepository;
        this.orgScopeService = orgScopeService;
        this.orgPersistenceGuard = orgPersistenceGuard;
    }

    public List<OrgUnitTreeNode> getUnitTree(Long currentPersonId) {
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        return orgScopeService.buildTree(scope.manageableUnits());
    }

    @Transactional(rollbackFor = Exception.class)
    public OrgUnitEntity createUnit(Long currentPersonId, CreateOrgUnitRequest request) {
        validateCreateRequest(request);
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        orgScopeService.assertUnitInScope(scope, request.parentId());
        OrgUnitEntity parent = orgUnitRepository.getById(request.parentId());
        if (parent == null) {
            throw new OrgValidationException("父级单位不存在");
        }
        if (orgUnitRepository.getByUnitCode(request.unitCode()) != null) {
            throw new OrgValidationException("单位编码已存在");
        }

        OrgUnitEntity entity = new OrgUnitEntity();
        entity.setUnitCode(request.unitCode().trim());
        entity.setUnitName(request.unitName().trim());
        entity.setParentId(parent.getId());
        entity.setAncestorPath(parent.getAncestorPath() + "/" + request.unitCode().trim());
        entity.setSortOrder(defaultSortOrder(request.sortOrder()));
        entity.setEnabled(request.enabled() == null || request.enabled());
        try {
            orgUnitRepository.save(entity);
            entity.setAncestorPath(parent.getAncestorPath() + "/" + entity.getId());
            orgUnitRepository.update(entity);
        } catch (RuntimeException exception) {
            orgPersistenceGuard.rethrowUnitCodeConflict(exception);
        }
        return orgUnitRepository.getById(entity.getId());
    }

    public OrgUnitEntity updateUnit(Long currentPersonId, Long unitId, UpdateOrgUnitRequest request) {
        validateUpdateRequest(request);
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        orgScopeService.assertUnitInScope(scope, unitId);
        OrgUnitEntity entity = requireUnit(unitId);
        OrgUnitEntity duplicate = orgUnitRepository.getByUnitCode(request.unitCode());
        if (duplicate != null && !Objects.equals(duplicate.getId(), unitId)) {
            throw new OrgValidationException("单位编码已存在");
        }
        entity.setUnitCode(request.unitCode().trim());
        entity.setUnitName(request.unitName().trim());
        entity.setSortOrder(defaultSortOrder(request.sortOrder()));
        entity.setEnabled(request.enabled() == null || request.enabled());
        try {
            orgUnitRepository.update(entity);
        } catch (RuntimeException exception) {
            orgPersistenceGuard.rethrowUnitCodeConflict(exception);
        }
        return orgUnitRepository.getById(unitId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUnit(Long currentPersonId, Long unitId) {
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        if (Objects.equals(scope.currentUnit().getId(), unitId)) {
            throw new OrgValidationException("不能删除当前人员所属单位");
        }
        orgScopeService.assertUnitInScope(scope, unitId);
        OrgUnitEntity entity = requireUnit(unitId);
        List<OrgUnitEntity> descendants = orgUnitRepository.findSelfAndDescendants(entity.getAncestorPath(), entity.getAncestorPath() + "/%");
        if (descendants.size() > 1) {
            throw new OrgValidationException("当前单位存在子单位，不能删除");
        }
        if (orgPersonRepository.countByUnitId(unitId) > 0) {
            throw new OrgValidationException("当前单位下存在人员，不能删除");
        }
        orgUnitRepository.removeById(unitId);
    }

    private OrgUnitEntity requireUnit(Long unitId) {
        OrgUnitEntity entity = orgUnitRepository.getById(unitId);
        if (entity == null) {
            throw new OrgValidationException("单位不存在");
        }
        return entity;
    }

    private void validateCreateRequest(CreateOrgUnitRequest request) {
        if (request == null || isBlank(request.unitCode()) || isBlank(request.unitName()) || request.parentId() == null) {
            throw new OrgValidationException("单位编码、名称和父级单位不能为空");
        }
    }

    private void validateUpdateRequest(UpdateOrgUnitRequest request) {
        if (request == null || isBlank(request.unitCode()) || isBlank(request.unitName())) {
            throw new OrgValidationException("单位编码和名称不能为空");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private int defaultSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }
}
