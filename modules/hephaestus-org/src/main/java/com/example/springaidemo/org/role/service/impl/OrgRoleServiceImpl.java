package com.example.springaidemo.org.role.service.impl;

import com.example.springaidemo.org.entity.OrgUnitEntity;
import com.example.springaidemo.org.exception.OrgValidationException;
import com.example.springaidemo.org.log.service.OperationLogRecorder;
import com.example.springaidemo.org.log.support.OperationLogRecordFactory;
import com.example.springaidemo.org.role.constant.OrgPermissionCodes;
import com.example.springaidemo.org.role.domain.OrgPermissionEntity;
import com.example.springaidemo.org.role.domain.OrgRoleEntity;
import com.example.springaidemo.org.role.domain.OrgRolePermissionEntity;
import com.example.springaidemo.org.role.dto.OrgPermissionItem;
import com.example.springaidemo.org.role.dto.OrgRoleRequest;
import com.example.springaidemo.org.role.dto.OrgRoleResponse;
import com.example.springaidemo.org.role.dto.OrgRoleTreeNode;
import com.example.springaidemo.org.role.dto.UpdateRolePeopleRequest;
import com.example.springaidemo.org.role.repository.OrgPermissionRepository;
import com.example.springaidemo.org.role.repository.OrgPersonRoleRepository;
import com.example.springaidemo.org.role.repository.OrgRolePermissionRepository;
import com.example.springaidemo.org.role.repository.OrgRoleRepository;
import com.example.springaidemo.org.role.service.OrgPersonPermissionRefreshService;
import com.example.springaidemo.org.role.service.OrgPermissionGuard;
import com.example.springaidemo.org.role.service.OrgRoleService;
import com.example.springaidemo.org.service.OrgScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrgRoleServiceImpl implements OrgRoleService {

    private final OrgRoleRepository roleRepository;
    private final OrgPermissionRepository permissionRepository;
    private final OrgRolePermissionRepository rolePermissionRepository;
    private final OrgPersonRoleRepository personRoleRepository;
    private final OrgScopeService orgScopeService;
    private final OrgPersonPermissionRefreshService permissionRefreshService;
    private final OrgPermissionGuard permissionGuard;
    private final OperationLogRecorder operationLogRecorder;
    private final OperationLogRecordFactory operationLogRecordFactory;

    public OrgRoleServiceImpl(OrgRoleRepository roleRepository,
                              OrgPermissionRepository permissionRepository,
                              OrgRolePermissionRepository rolePermissionRepository,
                              OrgPersonRoleRepository personRoleRepository,
                              OrgScopeService orgScopeService,
                              OrgPersonPermissionRefreshService permissionRefreshService,
                              OrgPermissionGuard permissionGuard,
                              OperationLogRecorder operationLogRecorder,
                              OperationLogRecordFactory operationLogRecordFactory) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.personRoleRepository = personRoleRepository;
        this.orgScopeService = orgScopeService;
        this.permissionRefreshService = permissionRefreshService;
        this.permissionGuard = permissionGuard;
        this.operationLogRecorder = operationLogRecorder;
        this.operationLogRecordFactory = operationLogRecordFactory;
    }

    @Override
    public List<OrgRoleTreeNode> getRoleTree(Long currentPersonId) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_ROLE_VIEW);
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        List<OrgRoleEntity> roles = roleRepository.findByScope(List.copyOf(scope.manageableUnitIds()), null, null, true);
        Map<Long, List<OrgRoleEntity>> rolesByUnit = roles.stream()
                .collect(Collectors.groupingBy(OrgRoleEntity::getUnitId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<OrgUnitEntity>> childrenMap = scope.manageableUnits().stream()
                .collect(Collectors.groupingBy(unit -> unit.getParentId() == null ? 0L : unit.getParentId(), LinkedHashMap::new, Collectors.toList()));
        childrenMap.values().forEach(children -> children.sort(unitComparator()));
        Set<Long> unitIds = scope.manageableUnitIds();
        return scope.manageableUnits().stream()
                .filter(unit -> !unitIds.contains(unit.getParentId()))
                .sorted(unitComparator())
                .map(unit -> toTreeNode(unit, childrenMap, rolesByUnit))
                .toList();
    }

    @Override
    public List<OrgRoleResponse> listRoles(Long currentPersonId, Long unitId, String keyword, Boolean enabled) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_ROLE_VIEW);
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        if (unitId != null && !scope.manageableUnitIds().contains(unitId)) {
            throw new OrgValidationException("岗位所属部门超出当前管理范围");
        }
        Map<Long, OrgUnitEntity> unitMap = scope.manageableUnits().stream()
                .collect(Collectors.toMap(OrgUnitEntity::getId, Function.identity()));
        return roleRepository.findByScope(List.copyOf(scope.manageableUnitIds()), unitId, keyword, enabled).stream()
                .map(role -> toResponse(role, unitMap.get(role.getUnitId()), List.of(), List.of()))
                .toList();
    }

    @Override
    public OrgRoleResponse getRole(Long currentPersonId, Long roleId) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_ROLE_VIEW);
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        OrgRoleEntity role = requireRoleInScope(scope, roleId);
        OrgUnitEntity unit = scope.manageableUnits().stream()
                .filter(item -> Objects.equals(item.getId(), role.getUnitId()))
                .findFirst()
                .orElse(null);
        return toResponse(role, unit, findRolePermissions(roleId), roleRepository.findPeopleByRoleId(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrgRoleResponse createRole(Long currentPersonId, OrgRoleRequest request) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_ROLE_CREATE);
        validateRequest(request);
        orgScopeService.assertUnitInScope(currentPersonId, request.unitId());
        OrgRoleEntity duplicate = roleRepository.getByRoleCode(request.roleCode().trim());
        if (duplicate != null) {
            throw new OrgValidationException("岗位编码已存在");
        }
        OrgRoleEntity role = new OrgRoleEntity();
        fillRole(role, request);
        roleRepository.save(role);
        replaceRolePermissions(role.getId(), request.permissionIds());
        recordRoleLog(currentPersonId, "create", "新增", role, "新增岗位：" + role.getRoleName(),
                "新增岗位“" + role.getRoleName() + "”，所属部门 ID 为“" + role.getUnitId() + "”，岗位类型为“" + blankText(role.getRoleType()) + "”。");
        return getRole(currentPersonId, role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrgRoleResponse updateRole(Long currentPersonId, Long roleId, OrgRoleRequest request) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_ROLE_UPDATE);
        validateRequest(request);
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        OrgRoleEntity role = requireRoleInScope(scope, roleId);
        orgScopeService.assertUnitInScope(scope, request.unitId());
        OrgRoleEntity duplicate = roleRepository.getByRoleCode(request.roleCode().trim());
        if (duplicate != null && !Objects.equals(duplicate.getId(), roleId)) {
            throw new OrgValidationException("岗位编码已存在");
        }
        fillRole(role, request);
        roleRepository.update(role);
        replaceRolePermissions(roleId, request.permissionIds());
        permissionRefreshService.refreshByRoleId(roleId);
        recordRoleLog(currentPersonId, "update", "修改", role, "修改岗位：" + role.getRoleName(),
                "修改岗位“" + role.getRoleName() + "”：岗位编码为“" + role.getRoleCode() + "”，岗位类型为“" + blankText(role.getRoleType()) + "”，启用状态为“" + enabledText(role.getEnabled()) + "”。");
        return getRole(currentPersonId, roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long currentPersonId, Long roleId) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_ROLE_DELETE);
        OrgRoleEntity role = requireRoleInScope(orgScopeService.resolveScope(currentPersonId), roleId);
        if (!personRoleRepository.findPersonIdsByRoleId(roleId).isEmpty()) {
            throw new OrgValidationException("岗位已绑定人员，不能删除");
        }
        rolePermissionRepository.deleteByRoleId(roleId);
        roleRepository.removeById(roleId);
        recordRoleLog(currentPersonId, "delete", "删除", role, "删除岗位：" + role.getRoleName(),
                "删除岗位“" + role.getRoleName() + "”。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrgRoleResponse updateRolePeople(Long currentPersonId, Long roleId, UpdateRolePeopleRequest request) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_ROLE_PERSON_ASSIGN);
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        OrgRoleEntity role = requireRoleInScope(scope, roleId);
        Set<Long> oldPersonIds = new LinkedHashSet<>(personRoleRepository.findPersonIdsByRoleId(roleId));
        Set<Long> newPersonIds = new LinkedHashSet<>(request == null || request.personIds() == null ? List.of() : request.personIds());
        for (Long personId : newPersonIds) {
            orgScopeService.requirePersonInScope(scope, personId);
        }
        personRoleRepository.deleteByRoleId(roleId);
        List<com.example.springaidemo.org.role.domain.OrgPersonRoleEntity> relations = newPersonIds.stream()
                .map(personId -> {
                    com.example.springaidemo.org.role.domain.OrgPersonRoleEntity entity = new com.example.springaidemo.org.role.domain.OrgPersonRoleEntity();
                    entity.setPersonId(personId);
                    entity.setRoleId(roleId);
                    return entity;
                })
                .toList();
        if (!relations.isEmpty()) {
            personRoleRepository.insertBatch(relations);
        }
        Set<Long> affected = new LinkedHashSet<>(oldPersonIds);
        affected.addAll(newPersonIds);
        permissionRefreshService.refreshPersonPermissions(affected);
        recordRoleLog(currentPersonId, "assign-person", "分配人员", role, "保存岗位人员：" + role.getRoleName(),
                "保存岗位“" + role.getRoleName() + "”的人员：新增人员 ID “" + diffText(newPersonIds, oldPersonIds) + "”，移除人员 ID “" + diffText(oldPersonIds, newPersonIds) + "”。");
        return getRole(currentPersonId, roleId);
    }

    @Override
    public void refreshRolePeoplePermissions(Long currentPersonId, Long roleId) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_ROLE_PERMISSION_REFRESH);
        OrgRoleEntity role = requireRoleInScope(orgScopeService.resolveScope(currentPersonId), roleId);
        permissionRefreshService.refreshByRoleId(roleId);
        recordRoleLog(currentPersonId, "refresh-permission", "刷新权限", role, "刷新岗位人员权限：" + role.getRoleName(),
                "刷新岗位“" + role.getRoleName() + "”下所有人员的权限。");
    }

    private OrgRoleEntity requireRoleInScope(OrgScopeService.ScopeContext scope, Long roleId) {
        OrgRoleEntity role = roleRepository.getById(roleId);
        if (role == null) {
            throw new OrgValidationException("岗位不存在");
        }
        if (!scope.manageableUnitIds().contains(role.getUnitId())) {
            throw new OrgValidationException("岗位超出当前管理范围");
        }
        return role;
    }

    private void recordRoleLog(Long operatorPersonId,
                               String actionCode,
                               String actionName,
                               OrgRoleEntity role,
                               String summary,
                               String detail) {
        operationLogRecorder.recordSuccess(operationLogRecordFactory.create(
                operatorPersonId,
                "org-role",
                "岗位",
                actionCode,
                actionName,
                "岗位",
                role == null ? null : role.getId(),
                role == null ? null : role.getRoleName(),
                summary,
                detail
        ));
    }

    private void validateRequest(OrgRoleRequest request) {
        if (request == null || isBlank(request.roleCode()) || isBlank(request.roleName()) || request.unitId() == null) {
            throw new OrgValidationException("岗位编码、岗位名称和所属部门不能为空");
        }
    }

    private void fillRole(OrgRoleEntity role, OrgRoleRequest request) {
        role.setRoleCode(request.roleCode().trim());
        role.setRoleName(request.roleName().trim());
        role.setRoleShortName(trimToNull(request.roleShortName()));
        role.setRoleDesc(trimToNull(request.roleDesc()));
        role.setUnitId(request.unitId());
        role.setRoleType(trimToNull(request.roleType()));
        role.setRoleGroup(trimToNull(request.roleGroup()));
        role.setRoleProperty(trimToNull(request.roleProperty()));
        role.setEnabled(request.enabled() == null || request.enabled());
        role.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private void replaceRolePermissions(Long roleId, Collection<Long> permissionIds) {
        rolePermissionRepository.deleteByRoleId(roleId);
        Set<Long> distinctIds = new LinkedHashSet<>(permissionIds == null ? List.of() : permissionIds);
        if (distinctIds.isEmpty()) {
            return;
        }
        Map<Long, OrgPermissionEntity> permissionMap = permissionRepository.findByIds(List.copyOf(distinctIds)).stream()
                .collect(Collectors.toMap(OrgPermissionEntity::getId, Function.identity()));
        List<OrgRolePermissionEntity> relations = distinctIds.stream()
                .map(permissionMap::get)
                .filter(Objects::nonNull)
                .map(permission -> {
                    OrgRolePermissionEntity entity = new OrgRolePermissionEntity();
                    entity.setRoleId(roleId);
                    entity.setPermissionId(permission.getId());
                    entity.setPermissionCode(permission.getPermissionCode());
                    return entity;
                })
                .toList();
        if (!relations.isEmpty()) {
            rolePermissionRepository.insertBatch(relations);
        }
    }

    private List<OrgPermissionItem> findRolePermissions(Long roleId) {
        List<Long> permissionIds = rolePermissionRepository.findByRoleId(roleId).stream()
                .map(OrgRolePermissionEntity::getPermissionId)
                .toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionRepository.findByIds(permissionIds).stream()
                .map(this::toPermissionItem)
                .toList();
    }

    private OrgRoleTreeNode toTreeNode(OrgUnitEntity unit,
                                       Map<Long, List<OrgUnitEntity>> childrenMap,
                                       Map<Long, List<OrgRoleEntity>> rolesByUnit) {
        List<OrgRoleTreeNode> children = new ArrayList<>();
        for (OrgUnitEntity child : childrenMap.getOrDefault(unit.getId(), List.of())) {
            children.add(toTreeNode(child, childrenMap, rolesByUnit));
        }
        rolesByUnit.getOrDefault(unit.getId(), List.of()).stream()
                .sorted(Comparator.comparing(role -> role.getSortOrder() == null ? 0 : role.getSortOrder()))
                .map(role -> new OrgRoleTreeNode("role-" + role.getId(), "role", role.getRoleName(), role.getUnitId(), role.getId(), List.of()))
                .forEach(children::add);
        return new OrgRoleTreeNode("unit-" + unit.getId(), "unit", unit.getUnitName(), unit.getId(), null, children);
    }

    private Comparator<OrgUnitEntity> unitComparator() {
        return Comparator
                .comparing((OrgUnitEntity unit) -> unit.getSortOrder() == null ? 0 : unit.getSortOrder())
                .thenComparing(OrgUnitEntity::getId);
    }

    private OrgRoleResponse toResponse(OrgRoleEntity role,
                                       OrgUnitEntity unit,
                                       List<OrgPermissionItem> permissions,
                                       List<com.example.springaidemo.org.role.dto.OrgRolePersonItem> people) {
        return new OrgRoleResponse(
                role.getId(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getRoleShortName(),
                role.getRoleDesc(),
                role.getUnitId(),
                unit == null ? null : unit.getUnitName(),
                role.getRoleType(),
                role.getRoleGroup(),
                role.getRoleProperty(),
                role.getEnabled(),
                role.getSortOrder(),
                permissions,
                people
        );
    }

    private OrgPermissionItem toPermissionItem(OrgPermissionEntity permission) {
        return new OrgPermissionItem(
                permission.getId(),
                permission.getPermissionCode(),
                permission.getPermissionName(),
                permission.getPermissionGroup(),
                permission.getPermissionSection(),
                permission.getSortOrder()
        );
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

    private String enabledText(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? "启用" : "停用";
    }

    private String diffText(Set<Long> source, Set<Long> baseline) {
        String value = source.stream()
                .filter(item -> !baseline.contains(item))
                .map(String::valueOf)
                .collect(Collectors.joining("、"));
        return value.isBlank() ? "无" : value;
    }
}
