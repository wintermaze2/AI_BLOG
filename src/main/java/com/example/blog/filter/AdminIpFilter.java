package com.example.blog.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * /admin/* 을 허용 IP에서만 접근할 수 있게 막는다. 인증(AuthFilter)보다 먼저 동작한다.
 *
 * <p><b>클라이언트 IP</b>는 {@code getRemoteAddr()}로 판단한다. 이 앱은 Nginx 뒤에 있으므로
 * Tomcat {@code server.xml}의 <b>RemoteIpValve</b>가 X-Forwarded-For를 반영해
 * 실제 클라이언트 IP를 채워주는 것에 의존한다. X-Forwarded-For 헤더를 앱에서 직접 읽지 않는 이유는,
 * 프록시 체인 검증 없이 그 헤더를 믿으면 누구나 값을 위조해 우회할 수 있기 때문이다.
 * RemoteIpValve는 internalProxies에 해당하는 요청에서만 헤더를 신뢰한다.
 *
 * <p><b>잠김 대비</b>: 밸브 설정이 잘못되어 있으면 모든 요청이 127.0.0.1로 보여 관리자 화면에
 * 아무도 못 들어간다. 그래서 (1) 차단할 때마다 관측된 IP를 로그로 남기고,
 * (2) 허용 목록을 환경변수 {@code BLOG_ADMIN_ALLOWED_IPS}(쉼표 구분)로 덮어쓸 수 있게 했다.
 * 재빌드 없이 systemd 환경변수만 고쳐 복구할 수 있다.
 *
 * <p>차단 응답은 403이 아니라 <b>404</b>다. 403은 "여기에 관리자 페이지가 있다"를 확인해 주는 셈이라,
 * 존재 자체를 숨기는 편이 낫다.
 */
public class AdminIpFilter implements Filter {

    /** 허용 IP를 덮어쓰는 환경변수(쉼표 구분). 미설정 시 아래 기본값 사용. */
    public static final String ENV_ALLOWED_IPS = "BLOG_ADMIN_ALLOWED_IPS";

    private static final String DEFAULT_ALLOWED_IPS = "211.239.43.40,58.121.175.130";

    private Set<String> allowedIps = Set.of();

    @Override
    public void init(FilterConfig filterConfig) {
        String raw = System.getenv(ENV_ALLOWED_IPS);
        boolean fromEnv = (raw != null && !raw.isBlank());
        if (!fromEnv) raw = DEFAULT_ALLOWED_IPS;

        allowedIps = parse(raw);
        System.err.println("[AdminIpFilter] 관리자 허용 IP " + allowedIps
                + (fromEnv ? " (환경변수 " + ENV_ALLOWED_IPS + ")" : " (기본값)"));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String ip = normalize(req.getRemoteAddr());
        if (!allowedIps.contains(ip)) {
            // 잠김 진단용: 실제로 관측된 IP를 남긴다. RemoteIpValve가 동작하지 않으면
            // 여기에 127.0.0.1 같은 값이 찍힌다.
            System.err.println("[AdminIpFilter] 차단 ip=" + ip + " path=" + req.getRequestURI());
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        chain.doFilter(request, response);
    }

    /** "a, b ,," -> {a, b} */
    private static Set<String> parse(String raw) {
        Set<String> set = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String ip = normalize(part);
            if (!ip.isEmpty()) set.add(ip);
        }
        return Collections.unmodifiableSet(set);
    }

    /** 앞뒤 공백 제거 + IPv4-mapped IPv6(::ffff:1.2.3.4)를 IPv4 표기로 통일. */
    private static String normalize(String ip) {
        if (ip == null) return "";
        String s = ip.trim();
        if (s.regionMatches(true, 0, "::ffff:", 0, 7)) s = s.substring(7);
        return s;
    }
}
