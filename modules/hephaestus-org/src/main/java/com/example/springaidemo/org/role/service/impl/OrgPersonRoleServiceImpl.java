package com.example.springaidemo.org.role.service.impl;

import com.example.springaidemo.org.entity.OrgPersonEntity;
import com.example.springaidemo.org.exception.OrgValidationException;
import com.example.springaidemo.org.log.service.OperationLogRecorder;
import com.example.springaidemo.org.log.support.OperationLogRecordFactory;
import com.example.springaidemo.org.role.constant.OrgPermissionCodes;
import com.example.springaidemo.org.role.domain.OrgPersonRoleEntity;
import com.example.springaidemo.org.role.domain.OrgRoleEntity;
import com.example.springaidemo.org.role.dto.OrgPersonRoleItem;
import com.example.springaidemo.org.role.dto.UpdatePersonRolesRequest;
import com.example.springaidemo.org.role.repository.OrgPersonRoleRepository;
import com.example.springaidemo.org.role.repository.OrgRoleRepository;
import com.example.springaidemo.org.role.service.OrgPermissionGuard;
import com.example.springaidemo.org.role.service.OrgPersonPermissionRefreshService;
import com.example.springaidemo.org.role.service.OrgPersonRoleService;
import com.example.springaidemo.org.service.OrgScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrgPersonRoleServiceImpl implements OrgPersonRoleService {

    private final OrgPersonRoleRepository personRoleRepository;
    private final OrgRoleRepository roleRepository;
    private final OrgScopeService orgScopeService;
    private final OrgPersonPermissionRefreshService permissionRefreshService;
    private final OrgPermissionGuard permissionGuard;
    private final OperationLogRecorder operationLogRecorder;
    private final OperationLogRecordFactory operationLogRecordFactory;

    public OrgPersonRoleServiceImpl(OrgPersonRoleRepository personRoleRepository,
                                    OrgRoleRepository roleRepository,
                                    OrgScopeService orgScopeService,
                                    OrgPersonPermissionRefreshService permissionRefreshService,
                                    OrgPermissionGuard permissionGuard,
                                    OperationLogRecorder operationLogRecorder,
                                    OperationLogRecordFactory operationLogRecordFactory) {
        this.personRoleRepository = personRoleRepository;
        this.roleRepository = roleRepository;
        this.orgScopeService = orgScopeService;
        this.permissionRefreshService = permissionRefreshService;
        this.permissionGuard = permissionGuard;
        this.operationLogRecorder = operationLogRecorder;
        this.operationLogRecordFactory = operationLogRecordFactory;
    }

    @Override
    public List<OrgPersonRoleItem> listPersonRoles(Long currentPersonId, Long personId) {
        orgScopeService.requirePersonInScope(currentPersonId, personId);
        return listPersonRolesForSummary(personId);
    }

    @Override
    public List<OrgPersonRoleItem> listPersonRolesForSummary(Long personId) {
        return roleRepository.findRolesByPersonId(personId);
    }

    @Override
    public Map<Long, List<OrgPersonRoleItem>> listPersonRolesForSummary(Collection<Long> personIds) {
        if (personIds == null || personIds.isEmpty()) {
            return Map.of();
        }
        List<Long> distinctPersonIds = personIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (distinctPersonIds.isEmpty()) {
            return Map.of();
        }
        return roleRepository.findRolesByPersonIds(distinctPersonIds).stream()
                .collect(Collectors.groupingBy(
                        row -> row.personId(),
                        LinkedHashMap::new,
                        Collectors.mapping(row -> row.toItem(), Collectors.toList())
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrgPersonRoleItem> replacePersonRoles(Long currentPersonId, Long personId, UpdatePersonRolesRequest request) {
        permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_ROLE_PERSON_ASSIGN);
        replacePersonRolesForPersonSave(currentPersonId, personId, request == null ? List.of() : request.roleIds());
        return listPersonRoles(currentPersonId, personId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePersonRolesForPersonSave(Long currentPersonId, Long personId, Collection<Long> roleIds) {
        OrgPersonEntity person = orgScopeService.requirePersonInScope(currentPersonId, personId);
        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(currentPersonId);
        Set<Long> distinctRoleIds = new LinkedHashSet<>(roleIds == null ? List.of() : roleIds);
        for (Long roleId : distinctRoleIds) {
            OrgRoleEntity role = roleRepository.getById(roleId);
            if (role == null) {
                throw new OrgValidationException("岗位不存在");
            }
            if (!Boolean.TRUE.equals(role.getEnabled())) {
                throw new OrgValidationException("岗位已停用");
            }
            if (!scope.manageableUnitIds().contains(role.getUnitId())) {
                throw new OrgValidationException("岗位超出当前管理范围");
            }
        }

        personRoleRepository.deleteByPersonId(person.getId());
        List<OrgPersonRoleEntity> relations = distinctRoleIds.stream()
                .map(roleId -> {
                    OrgPersonRoleEntity entity = new OrgPersonRoleEntity();
                    entity.setPersonId(person.getId());
                    entity.setRoleId(roleId);
                    return entity;
                })
                .toList();
        if (!relations.isEmpty()) {
            personRoleRepository.insertBatch(relations);
        }
        permissionRefreshService.refreshPersonPermissions(person.getId());
        operationLogRecorder.recordSuccess(operationLogRecordFactory.create(
                currentPersonId,
                "org-person",
                "人员",
                "assign-role",
                "分配岗位",
                "人员",
                person.getId(),
                person.getPersonName(),
                "保存人员岗位：" + person.getPersonName(),
                "保存人员“" + person.getPersonName() + "”的岗位，岗位 ID 为“" + roleIdText(distinctRoleIds) + "”。"
        ));
    }

    private String roleIdText(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return "无";
        }
        return roleIds.stream().map(String::valueOf).reduce((left, right) -> left + "、" + right).orElse("无");
    }
}
