package com.bank.account.common;

/**
 * 用户上下文(线程隔离)
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static String getUsername() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUsername();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 登录用户信息
     */
    public record LoginUser(Long userId, String username, String realName) {
        public Long getUserId() {
            return userId;
        }
        public String getUsername() {
            return username;
        }
    }
}
