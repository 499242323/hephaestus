package olympus.hephaestus.org.role.dto;

public record OrgPermissionItem(
        Long id,
        String permissionCode,
        String permissionName,
        String permissionGroup,
        String permissionSection,
        Integer sortOrder
) {
}
