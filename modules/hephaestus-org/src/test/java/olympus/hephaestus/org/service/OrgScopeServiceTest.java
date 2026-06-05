package olympus.hephaestus.org.service;

import olympus.hephaestus.org.entity.OrgPersonEntity;
import olympus.hephaestus.org.entity.OrgUnitEntity;
import olympus.hephaestus.org.exception.OrgAccessDeniedException;
import olympus.hephaestus.org.repository.OrgPersonRepository;
import olympus.hephaestus.org.repository.OrgUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgScopeServiceTest {

    @Mock
    private OrgPersonRepository orgPersonRepository;

    @Mock
    private OrgUnitRepository orgUnitRepository;

    @InjectMocks
    private OrgScopeService orgScopeService;

    @Test
    void shouldReturnCurrentUnitAndDescendantUnitIds() {
        OrgPersonEntity current = new OrgPersonEntity();
        current.setId(100L);
        current.setUnitId(1L);
        when(orgPersonRepository.getById(100L)).thenReturn(current);
        when(orgUnitRepository.findAllOrdered()).thenReturn(List.of(unit(1L, 0L, "1"), unit(2L, 1L, "1/2"), unit(9L, 0L, "9")));

        OrgScopeService.ScopeContext scope = orgScopeService.resolveScope(100L);

        assertThat(scope.manageableUnitIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void shouldRejectMissingCurrentPerson() {
        when(orgPersonRepository.getById(404L)).thenReturn(null);

        assertThatThrownBy(() -> orgScopeService.resolveScope(404L))
                .isInstanceOf(OrgAccessDeniedException.class)
                .hasMessageContaining("当前人员不存在");
    }

    private OrgUnitEntity unit(Long id, Long parentId, String ancestorPath) {
        OrgUnitEntity entity = new OrgUnitEntity();
        entity.setId(id);
        entity.setParentId(parentId);
        entity.setAncestorPath(ancestorPath);
        entity.setUnitCode("U" + id);
        entity.setUnitName("Unit " + id);
        entity.setSortOrder(0);
        entity.setEnabled(true);
        return entity;
    }
}
