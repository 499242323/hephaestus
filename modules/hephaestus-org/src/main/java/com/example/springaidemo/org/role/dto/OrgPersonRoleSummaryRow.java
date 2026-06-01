package com.example.springaidemo.org.role.dto;

public record OrgPersonRoleSummaryRow(
        Long personId,
        Long id,
        String roleCode,
        String roleName,
        Long unitId,
        String unitName
) {
    public OrgPersonRoleItem toItem() {
        return new OrgPersonRoleItem(id, roleCode, roleName, unitId, unitName);
    }
}
