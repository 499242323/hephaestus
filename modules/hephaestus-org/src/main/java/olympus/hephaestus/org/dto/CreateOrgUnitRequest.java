package olympus.hephaestus.org.dto;

public record CreateOrgUnitRequest(
        String unitCode,
        String unitName,
        Long parentId,
        Integer sortOrder,
        Boolean enabled
) {
}
