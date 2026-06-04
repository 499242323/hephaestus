package com.example.springaidemo.org.role.service;

import com.example.springaidemo.org.role.domain.OrgPersonPermissionEntity;
import com.example.springaidemo.org.role.domain.OrgRolePermissionEntity;
import com.example.springaidemo.org.role.repository.OrgPersonPermissionRepository;
import com.example.springaidemo.org.role.repository.OrgPersonRoleRepository;
import com.example.springaidemo.org.role.repository.OrgRolePermissionRepository;
import com.example.springaidemo.org.role.service.impl.OrgPersonPermissionRefreshServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrgPersonPermissionRefreshServiceImplTest {

    @Test
    void refreshPersonPermissionsRebuildsDistinctPermissionsFromAllRoles() {
        OrgPersonPermissionRepository personPermissionRepository = mock(OrgPersonPermissionRepository.class);
        OrgPersonRoleRepository personRoleRepository = mock(OrgPersonRoleRepository.class);
        OrgRolePermissionRepository rolePermissionRepository = mock(OrgRolePermissionRepository.class);
        OrgPersonPermissionRefreshService refreshService = new OrgPersonPermissionRefreshServiceImpl(
                personPermissionRepository,
                personRoleRepository,
                rolePermissionRepository
        );

        when(personRoleRepository.findRoleIdsByPersonId(100L)).thenReturn(List.of(10L, 11L));
        when(rolePermissionRepository.findByRoleIds(List.of(10L, 11L))).thenReturn(List.of(
                rolePermission(10L, 1L, "general.person.view"),
                rolePermission(10L, 2L, "general.person.update"),
                rolePermission(11L, 1L, "general.person.view"),
                rolePermission(11L, 3L, "general.person.delete")
        ));

        refreshService.refreshPersonPermissions(100L);

        verify(personPermissionRepository).deleteByPersonId(100L);
        ArgumentCaptor<List<OrgPersonPermissionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(personPermissionRepository).insertBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(OrgPersonPermissionEntity::getPersonId,
                        OrgPersonPermissionEntity::getPermissionId,
                        OrgPersonPermissionEntity::getPermissionCode)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(100L, 1L, "general.person.view"),
                        org.assertj.core.groups.Tuple.tuple(100L, 2L, "general.person.update"),
                        org.assertj.core.groups.Tuple.tuple(100L, 3L, "general.person.delete")
                );
    }

    private OrgRolePermissionEntity rolePermission(Long roleId, Long permissionId, String permissionCode) {
        OrgRolePermissionEntity entity = new OrgRolePermissionEntity();
        entity.setRoleId(roleId);
        entity.setPermissionId(permissionId);
        entity.setPermissionCode(permissionCode);
        return entity;
    }
}
