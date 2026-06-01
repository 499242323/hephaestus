package com.example.springaidemo.org.service;

import com.example.springaidemo.org.domain.OrgUnitTreeNode;
import com.example.springaidemo.org.dto.CreateOrgUnitRequest;
import com.example.springaidemo.org.dto.UpdateOrgUnitRequest;
import com.example.springaidemo.org.entity.OrgUnitEntity;
import com.example.springaidemo.org.exception.OrgValidationException;
import com.example.springaidemo.org.log.service.OperationLogRecorder;
import com.example.springaidemo.org.log.support.OperationLogRecordFactory;
import com.example.springaidemo.org.repository.OrgPersonRepository;
import com.example.springaidemo.org.repository.OrgUnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class OrgUnitService {

    private final OrgUnitRepository orgUnitRepository;
    private final OrgPersonRepository orgPersonRepository;
    private final OrgScopeService orgScopeService;
    private final OrgPersistenceGuard orgPersistenceGuard;
    private final OperationLogRecorder operationLogRecorder;
    private final OperationLogRecordFactory operationLogRecordFactory;

    public OrgUnitService(OrgUnitRepository orgUnitRepository,
                          OrgPersonRepository orgPersonRepository,
                          OrgScopeService orgScopeService,
                          OrgPersistenceGuard orgPersistenceGuard,
                          OperationLogRecorder operationLogRecorder,
                          OperationLogRecordFactory operationLogRecordFactory) {
        this.orgUnitRepository = orgUnitRepository;
        this.orgPersonRepository = orgPersonRepository;
        this.orgScopeService = orgScopeService;
        this.orgPersistenceGuard = orgPersistenceGuard;
        this.operationLogRecorder = operationLogRecorder;
        this.operationLogRecordFactory = operationLogRecordFactory;
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
        OrgUnitEntity parent = requireParentUnit(request.parentId());
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
            log.error("创建部门失败，currentPersonId={}, parentId={}, unitCode={}",
                    currentPersonId, request.parentId(), request.unitCode(), exception);
            orgPersistenceGuard.rethrowUnitCodeConflict(exception);
        }
        OrgUnitEntity created = orgUnitRepository.getById(entity.getId());
        recordUnitLog(currentPersonId, "create", "新增", created, "新增部门：" + created.getUnitName(),
                "新增部门“" + created.getUnitName() + "”，上级部门为“" + parent.getUnitName() + "”，排序为“" + created.getSortOrder() + "”。");
        return created;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrgUnitEntity updateUnit(Long currentPersonId, Long unitId, UpdateOrgUnitRequest request) {
        validateUpdateRequest(request);
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        orgScopeService.assertUnitInScope(scope, unitId);
        orgScopeService.assertUnitInScope(scope, request.parentId());

        OrgUnitEntity entity = requireUnit(unitId);
        OrgUnitEntity parent = requireParentUnit(request.parentId());
        validateParentChange(entity, parent);

        OrgUnitEntity duplicate = orgUnitRepository.getByUnitCode(request.unitCode());
        if (duplicate != null && !Objects.equals(duplicate.getId(), unitId)) {
            throw new OrgValidationException("单位编码已存在");
        }

        String originalAncestorPath = entity.getAncestorPath();
        String updatedAncestorPath = parent.getAncestorPath() + "/" + entity.getId();

        entity.setUnitCode(request.unitCode().trim());
        entity.setUnitName(request.unitName().trim());
        entity.setParentId(parent.getId());
        entity.setAncestorPath(updatedAncestorPath);
        entity.setSortOrder(defaultSortOrder(request.sortOrder()));
        entity.setEnabled(request.enabled() == null || request.enabled());
        try {
            orgUnitRepository.update(entity);
            refreshDescendantAncestorPaths(originalAncestorPath, updatedAncestorPath, unitId);
        } catch (RuntimeException exception) {
            log.error("更新部门失败，currentPersonId={}, unitId={}, parentId={}, unitCode={}",
                    currentPersonId, unitId, request.parentId(), request.unitCode(), exception);
            orgPersistenceGuard.rethrowUnitCodeConflict(exception);
        }
        OrgUnitEntity updated = orgUnitRepository.getById(unitId);
        recordUnitLog(currentPersonId, "update", "修改", updated, "修改部门：" + updated.getUnitName(),
                "修改部门“" + updated.getUnitName() + "”：部门编码为“" + updated.getUnitCode() + "”，上级部门为“" + parent.getUnitName() + "”，排序为“" + updated.getSortOrder() + "”。");
        return updated;
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
        recordUnitLog(currentPersonId, "delete", "删除", entity, "删除部门：" + entity.getUnitName(),
                "删除部门“" + entity.getUnitName() + "”。");
    }

    private void recordUnitLog(Long operatorPersonId,
                               String actionCode,
                               String actionName,
                               OrgUnitEntity unit,
                               String summary,
                               String detail) {
        operationLogRecorder.recordSuccess(operationLogRecordFactory.create(
                operatorPersonId,
                "org-unit",
                "部门",
                actionCode,
                actionName,
                "部门",
                unit == null ? null : unit.getId(),
                unit == null ? null : unit.getUnitName(),
                summary,
                detail
        ));
    }

    private OrgUnitEntity requireUnit(Long unitId) {
        OrgUnitEntity entity = orgUnitRepository.getById(unitId);
        if (entity == null) {
            throw new OrgValidationException("单位不存在");
        }
        return entity;
    }

    private OrgUnitEntity requireParentUnit(Long parentId) {
        OrgUnitEntity parent = orgUnitRepository.getById(parentId);
        if (parent == null) {
            throw new OrgValidationException("上级单位不存在");
        }
        return parent;
    }

    private void validateParentChange(OrgUnitEntity entity, OrgUnitEntity parent) {
        if (Objects.equals(entity.getId(), parent.getId())) {
            throw new OrgValidationException("上级单位不能是当前单位");
        }
        String currentPath = entity.getAncestorPath();
        String parentPath = parent.getAncestorPath();
        if (currentPath != null && parentPath != null && (parentPath.equals(currentPath) || parentPath.startsWith(currentPath + "/"))) {
            throw new OrgValidationException("上级单位不能是当前单位或其子单位");
        }
    }

    private void refreshDescendantAncestorPaths(String originalAncestorPath, String updatedAncestorPath, Long unitId) {
        if (originalAncestorPath == null || updatedAncestorPath == null || Objects.equals(originalAncestorPath, updatedAncestorPath)) {
            return;
        }
        List<OrgUnitEntity> descendants = orgUnitRepository.findSelfAndDescendants(originalAncestorPath, originalAncestorPath + "/%");
        for (OrgUnitEntity descendant : descendants) {
            if (Objects.equals(descendant.getId(), unitId)) {
                continue;
            }
            String ancestorPath = descendant.getAncestorPath();
            if (ancestorPath == null || !ancestorPath.startsWith(originalAncestorPath + "/")) {
                continue;
            }
            descendant.setAncestorPath(updatedAncestorPath + ancestorPath.substring(originalAncestorPath.length()));
            orgUnitRepository.update(descendant);
        }
    }

    private void validateCreateRequest(CreateOrgUnitRequest request) {
        if (request == null || isBlank(request.unitCode()) || isBlank(request.unitName()) || request.parentId() == null) {
            throw new OrgValidationException("单位编码、名称和上级单位不能为空");
        }
    }

    private void validateUpdateRequest(UpdateOrgUnitRequest request) {
        if (request == null || isBlank(request.unitCode()) || isBlank(request.unitName()) || request.parentId() == null) {
            throw new OrgValidationException("单位编码、名称和上级单位不能为空");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private int defaultSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }
}
