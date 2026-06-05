package olympus.hephaestus.org.role.dto;

import java.util.List;

public record OrgRoleResponse(
        Long id,
        String roleCode,
        String roleName,
        String roleShortName,
        String roleDesc,
        Long unitId,
        String unitName,
        String roleType,
        String roleGroup,
        String roleProperty,
        Boolean enabled,
        Integer sortOrder,
        List<OrgPermissionItem> permissions,
        List<OrgRolePersonItem> people
) {
}
