package com.example.springaidemo.org.role.service;

import java.util.Collection;

public interface OrgPersonPermissionRefreshService {

    void refreshPersonPermissions(Long personId);

    void refreshPersonPermissions(Collection<Long> personIds);

    void refreshByRoleId(Long roleId);
}
