package com.example.springaidemo.org.dto;

import com.example.springaidemo.org.domain.OrgPersonSummary;
import com.example.springaidemo.org.domain.OrgUnitTreeNode;

import java.util.List;

public record OrgScopeResponse(
        OrgPersonSummary currentPerson,
        List<OrgUnitTreeNode> units
) {
}
