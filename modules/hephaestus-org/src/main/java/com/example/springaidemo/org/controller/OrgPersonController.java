package com.example.springaidemo.org.controller;

import com.example.springaidemo.org.domain.OrgPersonSummary;
import com.example.springaidemo.org.dto.CreateOrgPersonRequest;
import com.example.springaidemo.org.dto.OrgScopeResponse;
import com.example.springaidemo.org.dto.UpdateOrgPersonRequest;
import com.example.springaidemo.org.service.OrgAvatarService;
import com.example.springaidemo.org.service.OrgPersonService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/org/persons")
public class OrgPersonController {

    private final OrgPersonService orgPersonService;
    private final OrgAvatarService orgAvatarService;

    public OrgPersonController(OrgPersonService orgPersonService, OrgAvatarService orgAvatarService) {
        this.orgPersonService = orgPersonService;
        this.orgAvatarService = orgAvatarService;
    }

    @GetMapping
    public List<OrgPersonSummary> list(@RequestHeader("X-Person-Id") Long personId,
                                       @RequestParam(value = "personName", required = false) String personName,
                                       @RequestParam(value = "unitId", required = false) Long unitId,
                                       @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return orgPersonService.listPersons(personId, personName, unitId, enabled);
    }

    @GetMapping("/current-scope")
    public OrgScopeResponse getCurrentScope(@RequestHeader("X-Person-Id") Long personId) {
        return orgPersonService.getCurrentScope(personId);
    }

    @PostMapping
    public OrgPersonSummary create(@RequestHeader("X-Person-Id") Long personId,
                                   @RequestBody CreateOrgPersonRequest request) {
        return orgPersonService.createPerson(personId, request);
    }

    @PutMapping("/{id}")
    public OrgPersonSummary update(@RequestHeader("X-Person-Id") Long personId,
                                   @PathVariable("id") Long id,
                                   @RequestBody UpdateOrgPersonRequest request) {
        return orgPersonService.updatePerson(personId, id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestHeader("X-Person-Id") Long personId,
                       @PathVariable("id") Long id) {
        orgPersonService.deletePerson(personId, id);
    }

    @PostMapping("/{id}/avatar")
    public OrgPersonSummary uploadAvatar(@RequestHeader("X-Person-Id") Long personId,
                                         @PathVariable("id") Long id,
                                         @RequestParam("file") MultipartFile file) {
        return orgAvatarService.bindAvatar(personId, id, file);
    }

    @DeleteMapping("/{id}/avatar")
    public OrgPersonSummary deleteAvatar(@RequestHeader("X-Person-Id") Long personId,
                                         @PathVariable("id") Long id) {
        return orgAvatarService.clearAvatar(personId, id);
    }
}
