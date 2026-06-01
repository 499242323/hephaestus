package com.example.springaidemo.login.config;

import java.util.List;

public final class LoginConfigConst {

    private LoginConfigConst() {
    }

    /** 主系统配置分组编码。 */
    public static final String GROUP_MAIN_SYSTEM = "main-system";

    /** 登录会话超时时间，单位：分钟。 */
    public static final String SESSION_TIMEOUT_MINUTES = "login.session.timeout.minutes";

    /** Single-session login switch. When enabled, a user keeps only the latest login session. */
    public static final String SESSION_SINGLE_LOGIN_ENABLED = "login.session.single-login.enabled";

    /** 是否启用登录密码前端 RSA 加密传输。 */
    public static final String PASSWORD_ENCRYPT_ENABLED = "login.password.encrypt.enabled";

    /** 前端密码加密算法名称。 */
    public static final String PASSWORD_ENCRYPT_ALGORITHM = "login.password.encrypt.algorithm";

    /** 后端密码解密 Cipher transformation。 */
    /** 登录密码 RSA 公钥，公开下发给登录页。 */
    public static final String PASSWORD_ENCRYPT_PUBLIC_KEY = "login.password.encrypt.public-key";

    /** 登录密码 RSA 私钥，仅服务端解密使用。 */
    public static final String PASSWORD_ENCRYPT_PRIVATE_KEY = "login.password.encrypt.private-key";

    /** 登录页标题。 */
    public static final String PAGE_TITLE = "login.page.title";

    /** 登录页副标题。 */
    public static final String PAGE_SUBTITLE = "login.page.subtitle";

    /** 登录页鼠标拖尾效果。 */
    public static final String MOUSE_TRAIL_EFFECT = "login.mouse.trail.effect";

    /** 登录页背景图 media_id。 */
    public static final String PAGE_BACKGROUND_MEDIA_ID = "login.page.background.media-id";

    /** 是否在登录页背景上叠加网格效果。 */
    public static final String PAGE_BACKGROUND_GRID_ENABLED = "login.page.background.grid.enabled";

    /** 登录 RSA 密钥配置项，用于启动时清理旧缓存。 */
    public static final List<String> PASSWORD_KEY_CONFIGS = List.of(
            PASSWORD_ENCRYPT_PUBLIC_KEY,
            PASSWORD_ENCRYPT_PRIVATE_KEY
    );
}
