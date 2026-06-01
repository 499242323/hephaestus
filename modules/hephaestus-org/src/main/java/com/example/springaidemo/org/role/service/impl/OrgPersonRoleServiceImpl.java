package com.example.springaidemo.org.role.service.impl;

import com.example.springaidemo.org.entity.OrgPersonEntity;
import com.example.springaidemo.org.exception.OrgValidationException;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrgPersonRoleServiceImpl implements OrgPersonRoleService {

    private final OrgPersonRoleRepository personRoleRepository;
    private final OrgRoleRepository roleRepository;
    private final OrgScopeService orgScopeService;
    private final OrgPersonPermissionRefreshService permissionRefreshService;
    private final OrgPermissionGuard permissionGuard;

    public OrgPersonRoleServiceImpl(OrgPersonRoleRepository personRoleRepository,
                                    OrgRoleRepository roleRepository,
                                    OrgScopeService orgScopeService,
                                    OrgPersonPermissionRefreshService permissionRefreshService,
                                    OrgPermissionGuard permissionGuard) {
        this.personRoleRepository = personRoleRepository;
        this.roleRepository = roleRepository;
        this.orgScopeService = orgScopeService;
        this.permissionRefreshService = permissionRefreshService;
        this.permissionGuard = permissionGuard;
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
    }
}
