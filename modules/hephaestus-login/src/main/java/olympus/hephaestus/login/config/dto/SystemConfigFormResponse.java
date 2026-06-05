package olympus.hephaestus.login.config.dto;

import java.util.List;

public record SystemConfigFormResponse(
        String groupCode,
        String groupName,
        List<SystemConfigSectionResponse> sections
) {
}
