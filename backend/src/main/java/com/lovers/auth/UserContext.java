package com.lovers.auth;

public class UserContext {

    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> openidHolder = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        userIdHolder.set(userId);
    }

    public static Long getUserId() {
        return userIdHolder.get();
    }

    public static void setOpenid(String openid) {
        openidHolder.set(openid);
    }

    public static String getOpenid() {
        return openidHolder.get();
    }

    public static void clear() {
        userIdHolder.remove();
        openidHolder.remove();
    }
}
