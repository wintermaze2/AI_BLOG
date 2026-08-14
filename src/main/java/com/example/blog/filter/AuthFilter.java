package com.example.blog.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * /admin/* 접근을 세션 기반으로 보호한다.
 * 로그인 페이지(/admin/login)는 예외로 통과시킨다.
 *
 * <p>매핑은 애노테이션이 아니라 <b>web.xml</b>에 선언되어 있다.
 * 애노테이션으로 선언한 필터들 사이의 실행 순서는 스펙상 보장되지 않는데,
 * IP 차단(AdminIpFilter)이 반드시 인증보다 먼저 돌아야 하기 때문이다.
 */
public class AuthFilter implements Filter {

    public static final String SESSION_USER = "adminUser";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());

        // 로그인 페이지는 인증 없이 허용
        if (path.equals("/admin/login")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        boolean loggedIn = session != null && session.getAttribute(SESSION_USER) != null;

        if (!loggedIn) {
            resp.sendRedirect(req.getContextPath() + "/admin/login");
            return;
        }
        chain.doFilter(request, response);
    }
}
