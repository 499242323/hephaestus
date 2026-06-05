package olympus.hephaestus.org.role.service;

import olympus.hephaestus.org.role.domain.OrgPersonPermissionEntity;
import olympus.hephaestus.org.role.repository.OrgPersonPermissionRepository;
import olympus.hephaestus.org.role.repository.OrgPersonRoleRepository;
import olympus.hephaestus.org.role.service.impl.OrgPermissionCache;
import olympus.hephaestus.org.role.service.impl.OrgPersonPermissionRefreshServiceImpl;
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
        OrgPermissionCache permissionCache = mock(OrgPermissionCache.class);
        OrgPersonPermissionRefreshService refreshService = new OrgPersonPermissionRefreshServiceImpl(
                personPermissionRepository,
                personRoleRepository,
                permissionCache
        );

        when(personPermissionRepository.findRolePermissionsByPersonIds(List.of(100L))).thenReturn(List.of(
                personPermission(100L, 1L, "general.person.view"),
                personPermission(100L, 2L, "general.person.update"),
                personPermission(100L, 1L, "general.person.view"),
                personPermission(100L, 3L, "general.person.delete")
        ));

        refreshService.refreshPersonPermissions(100L);

        verify(personPermissionRepository).deleteByPersonIds(List.of(100L));
        ArgumentCaptor<List<OrgPersonPermissionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(personPermissionRepository).insertBatch(captor.capture());
        verify(permissionCache).evictAfterCommit(100L);
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

    private OrgPersonPermissionEntity personPermission(Long personId, Long permissionId, String permissionCode) {
        OrgPersonPermissionEntity entity = new OrgPersonPermissionEntity();
        entity.setPersonId(personId);
        entity.setPermissionId(permissionId);
        entity.setPermissionCode(permissionCode);
        return entity;
    }
}
