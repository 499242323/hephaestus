package olympus.hephaestus.org.role.service;

import java.util.List;

public interface OrgPermissionGuard {

    boolean isAdmin(Long personId);

    boolean hasPermission(Long personId, String permissionCode);

    void requirePermission(Long personId, String permissionCode);

    List<String> listPermissionCodes(Long personId);
}
