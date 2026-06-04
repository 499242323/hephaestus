package com.example.springaidemo.login.log;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginLogCleanupNodeMatcherTest {

    private final LoginLogCleanupNodeMatcher matcher = new LoginLogCleanupNodeMatcher();

    @Test
    void shouldRunWhenEnabledAndCurrentNodeEqualsEnabledNode() {
        LoginLogCleanupProperties properties = new LoginLogCleanupProperties();
        properties.setEnabled(true);
        properties.setCurrentNode("app-01");
        properties.setEnabledNode("app-01");

        assertThat(matcher.shouldRun(properties)).isTrue();
    }

    @Test
    void shouldNotRunWhenDisabled() {
        LoginLogCleanupProperties properties = new LoginLogCleanupProperties();
        properties.setEnabled(false);
        properties.setCurrentNode("app-01");
        properties.setEnabledNode("app-01");

        assertThat(matcher.shouldRun(properties)).isFalse();
    }

    @Test
    void shouldNotRunWhenCurrentNodeDiffers() {
        LoginLogCleanupProperties properties = new LoginLogCleanupProperties();
        properties.setEnabled(true);
        properties.setCurrentNode("app-01");
        properties.setEnabledNode("app-02");

        assertThat(matcher.shouldRun(properties)).isFalse();
    }

    @Test
    void shouldTrimNodesBeforeMatching() {
        LoginLogCleanupProperties properties = new LoginLogCleanupProperties();
        properties.setEnabled(true);
        properties.setCurrentNode(" app-01 ");
        properties.setEnabledNode("app-01");

        assertThat(matcher.shouldRun(properties)).isTrue();
    }
}
