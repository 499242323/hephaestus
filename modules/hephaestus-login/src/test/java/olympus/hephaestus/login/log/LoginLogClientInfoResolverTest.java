package olympus.hephaestus.login.log;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LoginLogClientInfoResolverTest {

    private final LoginLogClientInfoResolver resolver = new LoginLogClientInfoResolver();

    @Test
    void resolveUsesFirstForwardedForAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader("X-Forwarded-For", "10.1.1.1, 10.1.1.2");
        request.addHeader("User-Agent", "Chrome");

        LoginLogClientInfo info = resolver.resolve(request);

        assertThat(info.clientIp()).isEqualTo("10.1.1.1");
        assertThat(info.userAgent()).isEqualTo("Chrome");
        assertThat(info.requestUri()).isEqualTo("/auth/login");
    }

    @Test
    void resolveUsesRealIpWhenForwardedForIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader("X-Real-IP", "10.2.2.2");

        LoginLogClientInfo info = resolver.resolve(request);

        assertThat(info.clientIp()).isEqualTo("10.2.2.2");
    }

    @Test
    void resolveFallsBackToRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("127.0.0.1");

        LoginLogClientInfo info = resolver.resolve(request);

        assertThat(info.clientIp()).isEqualTo("127.0.0.1");
    }
}
