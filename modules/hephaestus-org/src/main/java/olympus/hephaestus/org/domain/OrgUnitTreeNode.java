package olympus.hephaestus.org.domain;

import java.util.List;

public record OrgUnitTreeNode(
        Long id,
        String unitCode,
        String unitName,
        Long parentId,
        Integer sortOrder,
        Boolean enabled,
        List<OrgUnitTreeNode> children
) {
}
