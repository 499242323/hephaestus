package olympus.hephaestus.org.service;

import olympus.hephaestus.org.dto.CreateOrgUnitRequest;
import olympus.hephaestus.org.entity.OrgPersonEntity;
import olympus.hephaestus.org.entity.OrgUnitEntity;
import olympus.hephaestus.org.exception.OrgValidationException;
import olympus.hephaestus.org.repository.OrgPersonRepository;
import olympus.hephaestus.org.repository.OrgUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgUnitServiceTest {

    @Mock
    private OrgUnitRepository orgUnitRepository;

    @Mock
    private OrgPersonRepository orgPersonRepository;

    @Mock
    private OrgScopeService orgScopeService;

    @InjectMocks
    private OrgUnitService orgUnitService;

    @Test
    void shouldRejectDeletingUnitWhenItHasChildren() {
        OrgScopeService.ScopeContext scope = new OrgScopeService.ScopeContext(
                person(100L, 99L),
                unit(99L, 0L, "99"),
                List.of(unit(1L, 0L, "1"), unit(99L, 0L, "99")),
                java.util.Set.of(1L, 99L)
        );
        when(orgScopeService.resolveScope(100L)).thenReturn(scope);
        when(orgUnitRepository.getById(1L)).thenReturn(unit(1L, 0L, "1"));
        when(orgUnitRepository.findSelfAndDescendants("1", "1/%")).thenReturn(List.of(unit(1L, 0L, "1"), unit(2L, 1L, "1/2")));

        assertThatThrownBy(() -> orgUnitService.deleteUnit(100L, 1L))
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("子单位");
    }

    @Test
    void shouldRejectCreateWhenParentMissing() {
        when(orgUnitRepository.getById(9L)).thenReturn(null);

        assertThatThrownBy(() -> orgUnitService.createUnit(100L, new CreateOrgUnitRequest("U9", "Unit 9", 9L, 0, true)))
                .isInstanceOf(OrgValidationException.class);
    }

    private OrgPersonEntity person(Long id, Long unitId) {
        OrgPersonEntity entity = new OrgPersonEntity();
        entity.setId(id);
        entity.setUnitId(unitId);
        return entity;
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
