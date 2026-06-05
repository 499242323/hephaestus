package olympus.hephaestus.login.auth.dto;

public record LoginRequest(String username, String password, boolean encrypted) {
}
