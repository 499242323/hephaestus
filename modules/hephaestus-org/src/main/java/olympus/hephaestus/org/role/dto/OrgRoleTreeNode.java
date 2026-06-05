package olympus.hephaestus.org.role.dto;

import java.util.List;

public record OrgRoleTreeNode(
        String id,
        String type,
        String label,
        Long unitId,
        Long roleId,
        List<OrgRoleTreeNode> children
) {
}
