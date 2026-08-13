package com.example.blog.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 절대 URL 생성 보조.
 *
 * slug에 한글이 허용되므로(SlugUtil 참고) canonical·OG·sitemap·RSS에 넣는 URL은
 * 경로 세그먼트를 퍼센트 인코딩해야 한다. 화면 링크(JSP)는 브라우저가 알아서 인코딩하지만,
 * 문서에 문자열로 박히는 절대 URL은 직접 처리해야 한다.
 */
public final class UrlUtil {

    private UrlUtil() {}

    /** 경로의 각 세그먼트를 퍼센트 인코딩한다. 구분자 '/'는 그대로 둔다. */
    public static String encodePath(String path) {
        if (path == null || path.isEmpty()) return "";
        String[] segments = path.split("/", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) sb.append('/');
            // URLEncoder는 폼 인코딩 기준이라 공백을 '+'로 바꾼다. 경로에서는 %20이 맞다.
            sb.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return sb.toString();
    }
}
