package olympus.hephaestus.login.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequiredInterceptorTest {

    @Test
    void preHandleAllowsConfiguredExactWhitelistPath() throws Exception {
        LoginAuthProperties properties = new LoginAuthProperties();
        properties.getWhitelist().setPaths(List.of("/auth/login"));
        LoginRequiredInterceptor interceptor = new LoginRequiredInterceptor(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void preHandleAllowsConfiguredAntWhitelistPath() throws Exception {
        LoginAuthProperties properties = new LoginAuthProperties();
        properties.getWhitelist().setPaths(List.of("/api/system-config/public/**"));
        LoginRequiredInterceptor interceptor = new LoginRequiredInterceptor(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system-config/public/main-system");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void preHandleUsesFallbackWhitelistWhenYmlPathsAreEmpty() throws Exception {
        LoginRequiredInterceptor interceptor = new LoginRequiredInterceptor(new LoginAuthProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void preHandleRejectsAnonymousApiOutsideWhitelist() throws Exception {
        LoginAuthProperties properties = new LoginAuthProperties();
        properties.getWhitelist().setPaths(List.of("/auth/login"));
        LoginRequiredInterceptor interceptor = new LoginRequiredInterceptor(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/chat/send");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("未登录");
    }
}
