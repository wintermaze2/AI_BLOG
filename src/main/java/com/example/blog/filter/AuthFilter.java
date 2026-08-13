package com.example.blog.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * /admin/* 접근을 세션 기반으로 보호한다.
 * 로그인 페이지(/admin/login)는 예외로 통과시킨다.
 */
@WebFilter(urlPatterns = {"/admin/*"})
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
