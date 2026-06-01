package com.example.springaidemo.org.controller;

import com.example.springaidemo.org.domain.OrgPersonSummary;
import com.example.springaidemo.org.dto.CreateOrgPersonRequest;
import com.example.springaidemo.org.dto.OrgScopeResponse;
import com.example.springaidemo.org.dto.UpdateOrgPersonRequest;
import com.example.springaidemo.org.service.OrgAvatarService;
import com.example.springaidemo.org.service.OrgPersonService;
import com.example.springaidemo.org.support.OrgCurrentPersonResolver;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final OrgCurrentPersonResolver currentPersonResolver;

    public OrgPersonController(OrgPersonService orgPersonService,
                               OrgAvatarService orgAvatarService,
                               OrgCurrentPersonResolver currentPersonResolver) {
        this.orgPersonService = orgPersonService;
        this.orgAvatarService = orgAvatarService;
        this.currentPersonResolver = currentPersonResolver;
    }

    @GetMapping
    public List<OrgPersonSummary> list(HttpSession session,
                                       @RequestParam(value = "personName", required = false) String personName,
                                       @RequestParam(value = "unitId", required = false) Long unitId,
                                       @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return orgPersonService.listPersonsFast(currentPersonResolver.currentPersonId(session), personName, unitId, enabled);
    }

    @GetMapping("/current-scope")
    public OrgScopeResponse getCurrentScope(HttpSession session) {
        return orgPersonService.getCurrentScope(currentPersonResolver.currentPersonId(session));
    }

    @PostMapping
    public OrgPersonSummary create(HttpSession session,
                                   @RequestBody CreateOrgPersonRequest request) {
        return orgPersonService.createPerson(currentPersonResolver.currentPersonId(session), request);
    }

    @PutMapping("/{id}")
    public OrgPersonSummary update(HttpSession session,
                                   @PathVariable("id") Long id,
                                   @RequestBody UpdateOrgPersonRequest request) {
        return orgPersonService.updatePerson(currentPersonResolver.currentPersonId(session), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(HttpSession session,
                       @PathVariable("id") Long id) {
        orgPersonService.deletePerson(currentPersonResolver.currentPersonId(session), id);
    }

    @PostMapping("/{id}/avatar")
    public OrgPersonSummary uploadAvatar(HttpSession session,
                                         @PathVariable("id") Long id,
                                         @RequestParam("file") MultipartFile file) {
        return orgAvatarService.bindAvatar(currentPersonResolver.currentPersonId(session), id, file);
    }

    @DeleteMapping("/{id}/avatar")
    public OrgPersonSummary deleteAvatar(HttpSession session,
                                         @PathVariable("id") Long id) {
        return orgAvatarService.clearAvatar(currentPersonResolver.currentPersonId(session), id);
    }
}
