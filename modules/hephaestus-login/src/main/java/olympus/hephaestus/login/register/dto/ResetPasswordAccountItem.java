package olympus.hephaestus.login.register.dto;

public record ResetPasswordAccountItem(
        Long personId,
        String username,
        String personName,
        String sourceType
) {
}
