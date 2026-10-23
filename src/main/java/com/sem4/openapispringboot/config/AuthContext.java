package com.sem4.openapispringboot.config;

public class AuthContext {

    public record UserPrincipal(String email, Long customerId, String role) {}

    private static final ThreadLocal<UserPrincipal> CURRENT_USER = new ThreadLocal<>();

    public static void set(UserPrincipal principal) {
        CURRENT_USER.set(principal);
    }

    public static UserPrincipal get() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
