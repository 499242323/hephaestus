package olympus.hephaestus.login.log.support;

import olympus.hephaestus.login.log.dto.LoginLogClientInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LoginLogClientInfoResolver {

    public LoginLogClientInfo resolve(HttpServletRequest request) {
        return new LoginLogClientInfo(resolveClientIp(request), request.getHeader("User-Agent"), request.getRequestURI());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            for (String part : forwardedFor.split(",")) {
                if (StringUtils.hasText(part)) {
                    return part.trim();
                }
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        String remoteAddress = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddress) ? remoteAddress : "unknown";
    }
}
