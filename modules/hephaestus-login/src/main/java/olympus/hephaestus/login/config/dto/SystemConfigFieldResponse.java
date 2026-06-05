package olympus.hephaestus.login.config.dto;

import java.util.List;

public record SystemConfigFieldResponse(
        String code,
        String label,
        String componentType,
        String value,
        String defaultValue,
        boolean required,
        boolean publicReadable,
        boolean sensitive,
        String placeholder,
        String helpText,
        List<SystemConfigOptionResponse> options
) {
}
