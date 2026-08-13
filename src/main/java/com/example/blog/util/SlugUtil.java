package com.example.blog.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 제목을 URL 친화적인 slug로 변환한다.
 * 한글은 그대로 두되(퍼머링크에 한글 허용), 공백/특수문자는 하이픈으로 정리한다.
 */
public final class SlugUtil {

    private SlugUtil() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) return "post";
        String s = Normalizer.normalize(input.trim(), Normalizer.Form.NFC);
        s = s.toLowerCase(Locale.ROOT);
        // 허용: 한글, 영문, 숫자, 공백, 하이픈
        s = s.replaceAll("[^\\p{IsHangul}a-z0-9\\s-]", "");
        s = s.replaceAll("[\\s-]+", "-");
        s = s.replaceAll("(^-|-$)", "");
        return s.isBlank() ? "post" : s;
    }
}
