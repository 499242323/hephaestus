package olympus.hephaestus.org.dto;

import olympus.hephaestus.org.domain.OrgPersonSummary;
import olympus.hephaestus.org.domain.OrgUnitTreeNode;

import java.util.List;

public record OrgScopeResponse(
        OrgPersonSummary currentPerson,
        List<OrgUnitTreeNode> units
) {
}
