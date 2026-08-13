package com.example.blog.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt 기반 비밀번호 해시/검증.
 */
public final class PasswordUtil {

    private PasswordUtil() {}

    /** 평문 비밀번호 -> BCrypt 해시 */
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    /** 평문과 저장된 해시 비교 */
    public static boolean verify(String plain, String hash) {
        if (plain == null || hash == null || hash.isBlank()) return false;
        try {
            return BCrypt.checkpw(plain, hash);
        } catch (IllegalArgumentException e) {
            // 저장된 해시 형식이 잘못된 경우
            return false;
        }
    }
}
