package olympus.hephaestus.org.domain;

public record OrgPersonListRow(
        Long id,
        String personCode,
        String personName,
        String username,
        String password,
        Long unitId,
        String unitName,
        Long avatarMediaId,
        String avatarAccessUrl,
        String mobile,
        String email,
        String sourceType,
        String remark,
        Boolean enabled
) {
}
