package olympus.hephaestus.org.role.dto;

public record OrgPersonRoleItem(
        Long id,
        String roleCode,
        String roleName,
        Long unitId,
        String unitName
) {
}
