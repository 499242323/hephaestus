package olympus.hephaestus.org.role.dto;

import java.util.List;

public record OrgRoleRequest(
        String roleCode,
        String roleName,
        String roleShortName,
        String roleDesc,
        Long unitId,
        String roleType,
        String roleGroup,
        String roleProperty,
        Boolean enabled,
        Integer sortOrder,
        List<Long> permissionIds
) {
}
