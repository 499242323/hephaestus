package com.example.springaidemo.org.controller;

import com.example.springaidemo.org.domain.OrgUnitTreeNode;
import com.example.springaidemo.org.dto.CreateOrgUnitRequest;
import com.example.springaidemo.org.dto.UpdateOrgUnitRequest;
import com.example.springaidemo.org.entity.OrgUnitEntity;
import com.example.springaidemo.org.service.OrgUnitService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/org/units")
public class OrgUnitController {

    private final OrgUnitService orgUnitService;

    public OrgUnitController(OrgUnitService orgUnitService) {
        this.orgUnitService = orgUnitService;
    }

    @GetMapping("/tree")
    public List<OrgUnitTreeNode> getTree(@RequestHeader("X-Person-Id") Long personId) {
        return orgUnitService.getUnitTree(personId);
    }

    @PostMapping
    public OrgUnitEntity create(@RequestHeader("X-Person-Id") Long personId,
                                @RequestBody CreateOrgUnitRequest request) {
        return orgUnitService.createUnit(personId, request);
    }

    @PutMapping("/{id}")
    public OrgUnitEntity update(@RequestHeader("X-Person-Id") Long personId,
                                @PathVariable("id") Long id,
                                @RequestBody UpdateOrgUnitRequest request) {
        return orgUnitService.updateUnit(personId, id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestHeader("X-Person-Id") Long personId,
                       @PathVariable("id") Long id) {
        orgUnitService.deleteUnit(personId, id);
    }
}
