package com.example.blog.util;

import com.example.blog.model.Post;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 구조화 데이터(JSON-LD) 생성.
 *
 * <p>라이브러리 없이 문자열로 만들되, 값은 전부 {@link #esc(String)} 를 거치게 해
 * 따옴표나 {@code </script>} 가 섞여 들어가 문서가 깨지는 일이 없도록 한다.
 * 이스케이프를 한 군데로 모으는 것이 이 클래스의 존재 이유다.
 */
public final class JsonLd {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private JsonLd() {}

    /** 글 상세: BlogPosting */
    public static String blogPosting(Post p, String url) {
        String pub = p.getPublishedAt() != null ? p.getPublishedAt().format(ISO) : "";
        String mod = p.getUpdatedAt() != null ? p.getUpdatedAt().format(ISO) : pub;
        String desc = firstNonBlank(p.getMetaDescription(), p.getSummary(), "");
        String site = SiteConfig.siteName();

        List<String> f = new ArrayList<>();
        f.add(kv("@context", "https://schema.org"));
        f.add(kv("@type", "BlogPosting"));
        f.add(kv("headline", p.getTitle()));
        if (!desc.isBlank()) f.add(kv("description", desc));
        f.add(kv("url", url));
        f.add(kv("mainEntityOfPage", url));
        if (!pub.isEmpty()) f.add(kv("datePublished", pub));
        if (!mod.isEmpty()) f.add(kv("dateModified", mod));

        String image = firstNonBlank(p.getThumbnailUrl(), SiteConfig.defaultOgImage(), "");
        if (!image.isBlank()) f.add(kv("image", image));

        if (p.getCategoryName() != null && !p.getCategoryName().isBlank()) {
            f.add(kv("articleSection", p.getCategoryName()));
        }
        if (p.getTags() != null && !p.getTags().isEmpty()) {
            List<String> names = new ArrayList<>();
            p.getTags().forEach(t -> names.add(t.getName()));
            f.add("\"keywords\":" + strArray(names));
        }

        f.add("\"author\":" + organization(site));
        f.add("\"publisher\":" + organization(site));
        return obj(f);
    }

    /** 홈: WebSite + 사이트 내 검색(SearchAction). 검색창 리치결과 후보가 된다. */
    public static String webSite() {
        String base = SiteConfig.baseUrl();
        String action = obj(List.of(
                kv("@type", "SearchAction"),
                "\"target\":" + obj(List.of(
                        kv("@type", "EntryPoint"),
                        kv("urlTemplate", base + "/search?q={search_term_string}"))),
                kv("query-input", "required name=search_term_string")));

        return obj(List.of(
                kv("@context", "https://schema.org"),
                kv("@type", "WebSite"),
                kv("name", SiteConfig.siteName()),
                kv("url", base + "/"),
                "\"potentialAction\":" + action));
    }

    /**
     * 이동 경로: BreadcrumbList.
     *
     * @param trail 홈을 제외한 단계들. 각 원소는 {이름, 절대URL}.
     */
    public static String breadcrumb(List<String[]> trail) {
        String base = SiteConfig.baseUrl();
        List<String> items = new ArrayList<>();
        items.add(listItem(1, "홈", base + "/"));
        int pos = 2;
        for (String[] step : trail) {
            items.add(listItem(pos++, step[0], step[1]));
        }
        return obj(List.of(
                kv("@context", "https://schema.org"),
                kv("@type", "BreadcrumbList"),
                "\"itemListElement\":[" + String.join(",", items) + "]"));
    }

    // ---- 조립 도우미 -----------------------------------------------------

    private static String listItem(int position, String name, String url) {
        return obj(List.of(
                kv("@type", "ListItem"),
                "\"position\":" + position,
                kv("name", name),
                kv("item", url)));
    }

    private static String organization(String name) {
        List<String> f = new ArrayList<>();
        f.add(kv("@type", "Organization"));
        f.add(kv("name", name));
        String logo = SiteConfig.logoUrl();
        if (!logo.isBlank()) {
            f.add("\"logo\":" + obj(List.of(kv("@type", "ImageObject"), kv("url", logo))));
        }
        return obj(f);
    }

    private static String obj(List<String> fields) {
        return "{" + String.join(",", fields) + "}";
    }

    private static String kv(String key, String value) {
        return "\"" + esc(key) + "\":\"" + esc(value) + "\"";
    }

    private static String strArray(List<String> values) {
        List<String> quoted = new ArrayList<>();
        for (String v : values) quoted.add("\"" + esc(v) + "\"");
        return "[" + String.join(",", quoted) + "]";
    }

    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) return c;
        }
        return "";
    }

    /**
     * JSON 문자열 값 이스케이프.
     * {@code </} 까지 막는 이유: 본문에 {@code </script>} 가 들어가면 브라우저가 그 지점에서
     * script 블록을 끝내버려 이후 마크업이 통째로 깨진다.
     */
    static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"'  -> sb.append("\\\"");
                case '\n', '\r' -> sb.append(' ');
                case '\t' -> sb.append("\\t");
                case '<'  -> sb.append(i + 1 < s.length() && s.charAt(i + 1) == '/' ? "\\u003C" : "<");
                default   -> {
                    if (c < 0x20) sb.append(' ');
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
