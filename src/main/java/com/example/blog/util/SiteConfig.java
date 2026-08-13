package com.example.blog.util;

/**
 * 사이트 전역 설정. canonical/OG/sitemap 에 쓰이는 절대 URL 기준값 등.
 * 운영 시 환경변수 BLOG_BASE_URL, BLOG_SITE_NAME 으로 덮어쓴다.
 */
public final class SiteConfig {

    private SiteConfig() {}

    public static String baseUrl() {
        return env("BLOG_BASE_URL", "https://your-domain.com");
    }

    public static String siteName() {
        return env("BLOG_SITE_NAME", "My Blog");
    }

    public static String siteDescription() {
        return env("BLOG_SITE_DESC", "Tomcat + JSP + MySQL 로 직접 만든 경량 블로그");
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
