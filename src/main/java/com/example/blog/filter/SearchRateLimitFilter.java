package com.example.blog.filter;

import com.example.blog.util.RateLimiter;
import com.example.blog.util.SiteConfig;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * /search 요청 속도 제한.
 *
 * <p>검색은 이 사이트에서 유일하게 <b>입력값으로 DB 전체 본문을 훑는</b> 경로다.
 * {@code LIKE '%...%'} 는 인덱스를 타지 못하므로 글이 늘수록 한 번의 요청이 비싸진다.
 * 즉 짧은 시간에 검색을 쏟아부으면 그 자체로 서버를 압박할 수 있다(t4g.small, 2GB).
 * 그래서 클라이언트 IP 단위로 토큰 버킷 제한을 건다.
 *
 * <p>한도를 넘으면 <b>429 Too Many Requests</b> 와 {@code Retry-After} 헤더를 주고
 * 사이트 디자인이 적용된 안내 화면을 보여준다.
 *
 * <p>설정(환경변수, 미설정 시 기본값):
 * <ul>
 *   <li>{@code BLOG_SEARCH_RATE_PER_MIN} — 지속 가능한 분당 검색 수 (기본 20)</li>
 *   <li>{@code BLOG_SEARCH_RATE_BURST}   — 한 번에 몰아서 허용할 검색 수 (기본 10)</li>
 * </ul>
 *
 * <p>클라이언트 IP는 {@code getRemoteAddr()} 로 판단하며, Nginx 뒤이므로
 * Tomcat RemoteIpValve 가 실제 IP를 채워주는 것에 의존한다(AdminIpFilter 와 동일).
 * 밸브가 없으면 모든 요청이 한 IP로 보여 전체가 한 버킷을 공유하게 된다.
 */
@WebFilter(urlPatterns = {"/search"})
public class SearchRateLimitFilter implements Filter {

    public static final String ENV_PER_MIN = "BLOG_SEARCH_RATE_PER_MIN";
    public static final String ENV_BURST   = "BLOG_SEARCH_RATE_BURST";

    private static final double DEFAULT_PER_MIN = 20;
    private static final double DEFAULT_BURST   = 10;

    /** jakarta.servlet 에 429 상수가 없어 직접 정의한다. */
    private static final int SC_TOO_MANY_REQUESTS = 429;

    /** 추적할 최대 IP 수와 유휴 정리 기준. 메모리 상한 역할을 한다. */
    private static final int  MAX_TRACKED_IPS = 20_000;
    private static final long IDLE_EVICT_MS   = 10 * 60_000L;

    private RateLimiter limiter;
    private final AtomicLong blockedCount = new AtomicLong();

    @Override
    public void init(FilterConfig filterConfig) {
        double perMin = envDouble(ENV_PER_MIN, DEFAULT_PER_MIN);
        double burst  = envDouble(ENV_BURST,   DEFAULT_BURST);
        limiter = new RateLimiter(burst, perMin, MAX_TRACKED_IPS, IDLE_EVICT_MS);
        System.err.println("[SearchRateLimit] 분당 " + (long) perMin
                + "회, 버스트 " + (long) burst + "회");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String ip = normalizeIp(req.getRemoteAddr());
        long waitMs = limiter.acquireOrWaitMs(ip, System.currentTimeMillis());

        if (waitMs <= 0) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSec = Math.max(1, (waitMs + 999) / 1000);
        long n = blockedCount.incrementAndGet();
        // 공격 중에는 로그가 폭주하므로 처음 한 번과 100회마다만 남긴다.
        if (n == 1 || n % 100 == 0) {
            System.err.println("[SearchRateLimit] 차단 " + n + "건째 ip=" + ip
                    + " retryAfter=" + retryAfterSec + "s tracked=" + limiter.trackedKeys());
        }

        resp.setStatus(SC_TOO_MANY_REQUESTS);
        resp.setHeader("Retry-After", String.valueOf(retryAfterSec));
        resp.setHeader("Cache-Control", "no-store");

        req.setAttribute("baseUrl", SiteConfig.baseUrl());
        req.setAttribute("siteName", SiteConfig.siteName());
        req.setAttribute("pageTitle", "요청이 너무 잦습니다 | " + SiteConfig.siteName());
        req.setAttribute("noindex", true);
        req.setAttribute("errorMessage",
                "검색 요청이 너무 잦습니다. " + retryAfterSec + "초 후에 다시 시도해 주세요.");
        req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
    }

    private static double envDouble(String key, double def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return def;
        try {
            double d = Double.parseDouble(v.trim());
            return d > 0 ? d : def;
        } catch (NumberFormatException e) {
            System.err.println("[SearchRateLimit] " + key + " 값이 숫자가 아님: " + v + " -> 기본값 " + def);
            return def;
        }
    }

    /** IPv4-mapped IPv6(::ffff:1.2.3.4)를 IPv4 표기로 통일. AdminIpFilter 와 같은 규칙. */
    private static String normalizeIp(String ip) {
        if (ip == null) return "";
        String s = ip.trim();
        if (s.regionMatches(true, 0, "::ffff:", 0, 7)) s = s.substring(7);
        return s;
    }
}
