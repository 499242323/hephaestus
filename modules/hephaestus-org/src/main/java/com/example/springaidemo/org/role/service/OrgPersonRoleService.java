package com.example.springaidemo.org.role.service;

import com.example.springaidemo.org.role.dto.OrgPersonRoleItem;
import com.example.springaidemo.org.role.dto.UpdatePersonRolesRequest;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface OrgPersonRoleService {

    List<OrgPersonRoleItem> listPersonRoles(Long currentPersonId, Long personId);

    List<OrgPersonRoleItem> listPersonRolesForSummary(Long personId);

    Map<Long, List<OrgPersonRoleItem>> listPersonRolesForSummary(Collection<Long> personIds);

    List<OrgPersonRoleItem> replacePersonRoles(Long currentPersonId, Long personId, UpdatePersonRolesRequest request);

    void replacePersonRolesForPersonSave(Long currentPersonId, Long personId, Collection<Long> roleIds);
}
