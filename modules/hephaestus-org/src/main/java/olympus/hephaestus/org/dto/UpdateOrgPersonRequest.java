package olympus.hephaestus.org.dto;

import java.util.List;

public record UpdateOrgPersonRequest(
        String personCode,
        String personName,
        String username,
        String password,
        Long unitId,
        String mobile,
        String email,
        String remark,
        Boolean enabled,
        List<Long> roleIds
) {
}
