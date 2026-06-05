package olympus.hephaestus.login.config.dto;

import java.util.List;

public record SystemConfigSectionResponse(
        String sectionCode,
        String sectionName,
        List<SystemConfigFieldResponse> fields
) {
}
