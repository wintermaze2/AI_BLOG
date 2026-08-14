package com.example.blog.controller;

import com.example.blog.dao.AdminUserDao;
import com.example.blog.dao.CategoryDao;
import com.example.blog.dao.PostDao;
import com.example.blog.dao.TagDao;
import com.example.blog.filter.AuthFilter;
import com.example.blog.model.AdminUser;
import com.example.blog.model.Post;
import com.example.blog.util.MarkdownUtil;
import com.example.blog.util.PasswordUtil;
import com.example.blog.util.SlugUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 관리자 라우팅.
 *   GET  /admin              대시보드(글 목록, DRAFT 포함)
 *   GET  /admin/login        로그인 폼
 *   POST /admin/login        인증
 *   GET  /admin/logout       로그아웃
 *   GET  /admin/new          새 글 폼
 *   GET  /admin/edit?id=..   수정 폼
 *   POST /admin/save         저장(신규/수정)
 *   POST /admin/delete       삭제
 */
@WebServlet(urlPatterns = {"/admin", "/admin/*"})
public class AdminServlet extends HttpServlet {

    private final PostDao postDao = new PostDao();
    private final AdminUserDao adminUserDao = new AdminUserDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final TagDao tagDao = new TagDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = action(req);
        switch (action) {
            case "/login"  -> forward(req, resp, "/WEB-INF/views/admin/login.jsp");
            case "/logout" -> logout(req, resp);
            case "/new"    -> newForm(req, resp);
            case "/edit"   -> editForm(req, resp);
            default        -> dashboard(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = action(req);
        switch (action) {
            case "/login"  -> login(req, resp);
            case "/save"   -> save(req, resp);
            case "/delete" -> delete(req, resp);
            default        -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ---- 화면 ----------------------------------------------------------

    private void dashboard(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        ensureCsrf(req); // 목록의 삭제 폼에서 사용
        List<Post> posts = postDao.findAllForAdmin(0, 200);
        req.setAttribute("posts", posts);
        forward(req, resp, "/WEB-INF/views/admin/list.jsp");
    }

    private void newForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        ensureCsrf(req);
        req.setAttribute("categories", categoryDao.findAll());
        forward(req, resp, "/WEB-INF/views/admin/form.jsp");
    }

    private void editForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseLong(req.getParameter("id"), 0);
        Post post = postDao.findByIdFull(id);
        if (post == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        post.setTags(tagDao.findByPostId(post.getId()));
        ensureCsrf(req);
        req.setAttribute("post", post);
        req.setAttribute("categories", categoryDao.findAll());
        forward(req, resp, "/WEB-INF/views/admin/form.jsp");
    }

    // ---- 인증 ----------------------------------------------------------

    private void login(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = trim(req.getParameter("username"));
        String password = req.getParameter("password");

        AdminUser user = (username == null) ? null : adminUserDao.findByUsername(username);
        if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            req.setAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            forward(req, resp, "/WEB-INF/views/admin/login.jsp");
            return;
        }
        // 세션 고정 공격 방지: 세션 ID 재발급
        req.changeSessionId();
        HttpSession session = req.getSession(true);
        session.setAttribute(AuthFilter.SESSION_USER, user.getUsername());
        resp.sendRedirect(req.getContextPath() + "/admin");
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        resp.sendRedirect(req.getContextPath() + "/admin/login");
    }

    // ---- 저장/삭제 -----------------------------------------------------

    private void save(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!checkCsrf(req)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF"); return; }

        long id       = parseLong(req.getParameter("id"), 0);
        String title  = trim(req.getParameter("title"));
        String slug   = trim(req.getParameter("slug"));
        String summary = trim(req.getParameter("summary"));
        // contentMd 파라미터는 형식과 무관하게 "본문 원문"을 담는다(MD면 마크다운, HTML이면 HTML 소스).
        String source = req.getParameter("contentMd");
        String contentType = "HTML".equals(req.getParameter("contentType")) ? "HTML" : "MD";
        String thumb  = trim(req.getParameter("thumbnailUrl"));
        String meta   = trim(req.getParameter("metaDescription"));
        String status = "PUBLISHED".equals(req.getParameter("status")) ? "PUBLISHED" : "DRAFT";
        Long categoryId = optLong(req.getParameter("categoryId"));
        List<String> tagNames = TagDao.parseNames(req.getParameter("tags"));

        if (title == null || title.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "제목은 필수입니다.");
            return;
        }
        // slug 미입력 시 제목에서 생성
        if (slug == null || slug.isBlank()) slug = SlugUtil.toSlug(title);
        // slug 중복 방지
        if (postDao.slugExists(slug, id)) slug = slug + "-" + System.currentTimeMillis();

        Post p = (id > 0) ? postDao.findByIdFull(id) : new Post();
        if (p == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

        p.setId(id);
        p.setTitle(title);
        p.setSlug(slug);
        p.setSummary(summary);
        // 원문은 형식과 무관하게 content_md에 보관한다(재편집 시 그대로 다시 보여주기 위함).
        // 렌더링본(content_html)은 MD면 변환하고, HTML이면 입력을 그대로 쓴다.
        p.setContentMd(source);
        p.setContentType(contentType);
        p.setContentHtml("HTML".equals(contentType)
                ? (source == null ? "" : source)
                : MarkdownUtil.toHtml(source));
        p.setThumbnailUrl(thumb);
        p.setMetaDescription(meta);
        p.setStatus(status);
        p.setCategoryId(categoryId);

        // 발행 시각: PUBLISHED 로 처음 넘어갈 때 현재 시각 설정
        if ("PUBLISHED".equals(status) && p.getPublishedAt() == null) {
            p.setPublishedAt(LocalDateTime.now());
        }

        long savedId;
        if (id > 0) {
            postDao.update(p);
            savedId = id;
        } else {
            savedId = postDao.insert(p);
        }
        if (savedId > 0) tagDao.syncPostTags(savedId, tagNames);

        resp.sendRedirect(req.getContextPath() + "/admin");
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!checkCsrf(req)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF"); return; }
        long id = parseLong(req.getParameter("id"), 0);
        if (id > 0) {
            postDao.delete(id);          // post_tag는 FK CASCADE로 함께 삭제됨
            tagDao.deleteOrphans();      // 그 결과 어디에도 안 붙은 태그 정리
        }
        resp.sendRedirect(req.getContextPath() + "/admin");
    }

    // ---- 유틸 ----------------------------------------------------------

    /** /admin, /admin/ 는 "", 그 외는 pathInfo(예: /login) 반환 */
    private String action(HttpServletRequest req) {
        String pi = req.getPathInfo();
        if (pi == null || pi.equals("/")) return "";
        return pi;
    }

    private void ensureCsrf(HttpServletRequest req) {
        HttpSession s = req.getSession(true);
        if (s.getAttribute("csrf") == null) {
            s.setAttribute("csrf", UUID.randomUUID().toString());
        }
    }

    private boolean checkCsrf(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return false;
        Object token = s.getAttribute("csrf");
        String sent = req.getParameter("csrf");
        return token != null && token.equals(sent);
    }

    private void forward(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.getRequestDispatcher(view).forward(req, resp);
    }

    private String trim(String s) { return s == null ? null : s.trim(); }

    private long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }

    private Long optLong(String s) {
        try { return (s == null || s.isBlank()) ? null : Long.parseLong(s); }
        catch (Exception e) { return null; }
    }
}
