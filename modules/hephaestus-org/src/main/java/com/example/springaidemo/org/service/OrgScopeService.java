package com.example.springaidemo.org.service;

import com.example.springaidemo.org.domain.OrgUnitTreeNode;
import com.example.springaidemo.org.entity.OrgPersonEntity;
import com.example.springaidemo.org.entity.OrgUnitEntity;
import com.example.springaidemo.org.exception.OrgAccessDeniedException;
import com.example.springaidemo.org.repository.OrgPersonRepository;
import com.example.springaidemo.org.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrgScopeService {

    private final OrgPersonRepository orgPersonRepository;
    private final OrgUnitRepository orgUnitRepository;

    public OrgScopeService(OrgPersonRepository orgPersonRepository, OrgUnitRepository orgUnitRepository) {
        this.orgPersonRepository = orgPersonRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    public ScopeContext resolveScope(Long currentPersonId) {
        OrgPersonEntity currentPerson = orgPersonRepository.getById(currentPersonId);
        if (currentPerson == null) {
            throw new OrgAccessDeniedException("当前人员不存在");
        }
        if (currentPerson.getUnitId() == null) {
            throw new OrgAccessDeniedException("当前人员未绑定单位");
        }

        List<OrgUnitEntity> allUnits = orgUnitRepository.findAllOrdered();
        OrgUnitEntity currentUnit = allUnits.stream()
                .filter(unit -> Objects.equals(unit.getId(), currentPerson.getUnitId()))
                .findFirst()
                .orElseThrow(() -> new OrgAccessDeniedException("当前人员所属单位不存在"));

        List<OrgUnitEntity> manageableUnits = allUnits.stream()
                .filter(unit -> isSelfOrDescendant(currentUnit.getAncestorPath(), unit.getAncestorPath()))
                .sorted(Comparator.comparing(OrgUnitEntity::getAncestorPath)
                        .thenComparing(unit -> unit.getSortOrder() == null ? 0 : unit.getSortOrder())
                        .thenComparing(OrgUnitEntity::getId))
                .toList();

        return new ScopeContext(
                currentPerson,
                currentUnit,
                manageableUnits,
                manageableUnits.stream().map(OrgUnitEntity::getId).collect(Collectors.toSet())
        );
    }

    public void assertUnitInScope(Long currentPersonId, Long unitId) {
        assertUnitInScope(resolveScope(currentPersonId), unitId);
    }

    public void assertUnitInScope(ScopeContext scope, Long unitId) {
        if (!scope.manageableUnitIds().contains(unitId)) {
            throw new OrgAccessDeniedException("目标单位超出当前管理范围");
        }
    }

    public OrgPersonEntity requirePersonInScope(Long currentPersonId, Long personId) {
        return requirePersonInScope(resolveScope(currentPersonId), personId);
    }

    public OrgPersonEntity requirePersonInScope(ScopeContext scope, Long personId) {
        OrgPersonEntity person = orgPersonRepository.getById(personId);
        if (person == null) {
            throw new OrgAccessDeniedException("目标人员不存在");
        }
        if (!scope.manageableUnitIds().contains(person.getUnitId())) {
            throw new OrgAccessDeniedException("目标人员超出当前管理范围");
        }
        return person;
    }

    public List<OrgUnitTreeNode> buildTree(List<OrgUnitEntity> units) {
        Map<Long, List<OrgUnitEntity>> childrenMap = units.stream()
                .collect(Collectors.groupingBy(unit -> unit.getParentId() == null ? 0L : unit.getParentId(), LinkedHashMap::new, Collectors.toList()));
        Set<Long> ids = units.stream().map(OrgUnitEntity::getId).collect(Collectors.toSet());

        return units.stream()
                .filter(unit -> !ids.contains(unit.getParentId()))
                .map(unit -> toTreeNode(unit, childrenMap))
                .toList();
    }

    private OrgUnitTreeNode toTreeNode(OrgUnitEntity unit, Map<Long, List<OrgUnitEntity>> childrenMap) {
        List<OrgUnitTreeNode> children = new ArrayList<>();
        for (OrgUnitEntity child : childrenMap.getOrDefault(unit.getId(), List.of())) {
            children.add(toTreeNode(child, childrenMap));
        }
        return new OrgUnitTreeNode(
                unit.getId(),
                unit.getUnitCode(),
                unit.getUnitName(),
                unit.getParentId(),
                unit.getSortOrder(),
                unit.getEnabled(),
                children
        );
    }

    private boolean isSelfOrDescendant(String currentPath, String targetPath) {
        if (currentPath == null || targetPath == null) {
            return false;
        }
        return targetPath.equals(currentPath) || targetPath.startsWith(currentPath + "/");
    }

    public record ScopeContext(
            OrgPersonEntity currentPerson,
            OrgUnitEntity currentUnit,
            List<OrgUnitEntity> manageableUnits,
            Set<Long> manageableUnitIds
    ) {
    }
}
