package olympus.hephaestus.login.log.dto;

public record LoginLogClientInfo(
        String clientIp,
        String userAgent,
        String requestUri
) {
}
