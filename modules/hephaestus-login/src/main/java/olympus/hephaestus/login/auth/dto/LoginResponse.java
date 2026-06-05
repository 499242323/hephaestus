package olympus.hephaestus.login.auth.dto;

import olympus.hephaestus.login.auth.domain.LoginSessionUser;

public record LoginResponse(boolean success, LoginSessionUser user, String message) {
}
