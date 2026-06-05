package olympus.hephaestus.org.support;

import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Method;

public final class SessionUtils {

    public static final String SESSION_USER_KEY = "HEPHAESTUS_LOGIN_USER";

    private SessionUtils() {
    }

    public static void setLoginUser(HttpSession session, Object user) {
        if (session != null) {
            session.setAttribute(SESSION_USER_KEY, user);
        }
    }

    public static Object getLoginUser(HttpSession session) {
        return session == null ? null : session.getAttribute(SESSION_USER_KEY);
    }

    public static <T> T getLoginUser(HttpSession session, Class<T> userType) {
        Object user = getLoginUser(session);
        return userType.isInstance(user) ? userType.cast(user) : null;
    }

    public static boolean hasLoginUser(HttpSession session, Class<?> userType) {
        return getLoginUser(session, userType) != null;
    }

    public static Long getPersonId(HttpSession session) {
        return resolvePersonId(getLoginUser(session));
    }

    private static Long resolvePersonId(Object sessionUser) {
        if (sessionUser == null) {
            return null;
        }
        try {
            Method method = sessionUser.getClass().getMethod("personId");
            Object value = method.invoke(sessionUser);
            return value instanceof Number number ? number.longValue() : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }
}
