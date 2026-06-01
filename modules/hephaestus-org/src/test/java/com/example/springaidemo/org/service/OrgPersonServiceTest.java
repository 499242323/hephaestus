package com.example.springaidemo.org.service;

import com.example.springaidemo.media.service.MediaFileService;
import com.example.springaidemo.org.dto.CreateOrgPersonRequest;
import com.example.springaidemo.org.entity.OrgPersonEntity;
import com.example.springaidemo.org.entity.OrgUnitEntity;
import com.example.springaidemo.org.exception.OrgAccessDeniedException;
import com.example.springaidemo.org.repository.OrgPersonRepository;
import com.example.springaidemo.org.repository.OrgUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class OrgPersonServiceTest {

    @Mock
    private OrgPersonRepository orgPersonRepository;

    @Mock
    private OrgUnitRepository orgUnitRepository;

    @Mock
    private OrgScopeService orgScopeService;

    @Mock
    private MediaFileService mediaFileService;

    @InjectMocks
    private OrgPersonService orgPersonService;

    @Test
    void shouldRejectCreatingPersonOutsideManageableUnit() {
        CreateOrgPersonRequest request = new CreateOrgPersonRequest("P-1", "Alice", "alice", "pass", 9L, null, null, null, true, List.of());
        doThrow(new OrgAccessDeniedException("目标单位超出当前管理范围"))
                .when(orgScopeService).assertUnitInScope(100L, 9L);

        assertThatThrownBy(() -> orgPersonService.createPerson(100L, request))
                .isInstanceOf(OrgAccessDeniedException.class)
                .hasMessageContaining("管理范围");
    }

    @Test
    void shouldRejectMissingUnitWhenCreatingPerson() {
        CreateOrgPersonRequest request = new CreateOrgPersonRequest("P-2", "Bob", "bob", "pass", 1L, null, null, null, true, List.of());
        org.mockito.Mockito.when(orgUnitRepository.getById(1L)).thenReturn(null);

        assertThatThrownBy(() -> orgPersonService.createPerson(100L, request))
                .isInstanceOf(RuntimeException.class);
    }
}
