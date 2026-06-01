package com.example.springaidemo.org.role.service;

import com.example.springaidemo.org.role.dto.OrgPersonRoleItem;
import com.example.springaidemo.org.role.dto.UpdatePersonRolesRequest;

import java.util.Collection;
import java.util.List;

public interface OrgPersonRoleService {

    List<OrgPersonRoleItem> listPersonRoles(Long currentPersonId, Long personId);

    List<OrgPersonRoleItem> listPersonRolesForSummary(Long personId);

    List<OrgPersonRoleItem> replacePersonRoles(Long currentPersonId, Long personId, UpdatePersonRolesRequest request);

    void replacePersonRolesForPersonSave(Long currentPersonId, Long personId, Collection<Long> roleIds);
}
