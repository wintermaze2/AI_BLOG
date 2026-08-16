package com.example.blog.util;

/**
 * 사이트 전역 설정. canonical/OG/sitemap/JSON-LD 에 쓰이는 값들.
 * 모두 환경변수로 덮어쓸 수 있고, 미설정 시 아래 기본값을 쓴다.
 */
public final class SiteConfig {

    private SiteConfig() {}

    public static String baseUrl() {
        return env("BLOG_BASE_URL", "https://your-domain.com");
    }

    public static String siteName() {
        return env("BLOG_SITE_NAME", "My Blog");
    }

    /**
     * 홈 화면의 meta description 이자 RSS 채널 설명.
     * 검색결과에 그대로 노출되므로 운영 환경에서는 반드시 BLOG_SITE_DESC 를 설정할 것.
     */
    public static String siteDescription() {
        return env("BLOG_SITE_DESC", "이메일 발송과 도달률에 대한 기술 블로그입니다.");
    }

    // =================================================================
    // 검색엔진 소유확인 (등록 시 발급받은 토큰을 환경변수로 넣는다)
    // =================================================================

    /** Google Search Console 의 HTML 태그 방식 토큰. 비어 있으면 meta 를 출력하지 않는다. */
    public static String googleSiteVerification() {
        return env("BLOG_GOOGLE_SITE_VERIFICATION", "");
    }

    /** 네이버 서치어드바이저 소유확인 토큰. */
    public static String naverSiteVerification() {
        return env("BLOG_NAVER_SITE_VERIFICATION", "");
    }

    // =================================================================
    // 이미지
    // =================================================================

    /**
     * 대표 이미지가 없는 글과 목록 화면에서 쓸 기본 og:image (절대 URL).
     * 미설정 시 og:image 를 아예 넣지 않는다 — 잘못된 크기의 이미지를 넣느니 없는 편이 낫다.
     * 권장 크기 1200x630.
     */
    public static String defaultOgImage() {
        return env("BLOG_DEFAULT_OG_IMAGE", "");
    }

    /**
     * JSON-LD publisher.logo 에 쓸 로고 (절대 URL).
     * 미설정 시 logo 를 넣지 않는다. 구글이 너무 작은 로고에 경고를 주므로
     * 최소 112px 이상을 준비한 뒤에 설정할 것. (파비콘용 로고는 50x37 이라 부적합)
     */
    public static String logoUrl() {
        return env("BLOG_LOGO_URL", "");
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
