package com.example.blog.controller;

import com.example.blog.dao.PostDao;
import com.example.blog.model.Post;
import com.example.blog.util.SiteConfig;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 프론트 컨트롤러: 모든 요청(정적 리소스 제외)을 받아 예쁜 URL을 분석해 라우팅한다.
 *
 *   /                     -> 글 목록(홈), ?page=N 페이징
 *   /posts/{slug}         -> 글 상세
 *   그 외                  -> 404
 *
 * 정적 리소스(/static/*)는 web.xml에서 컨테이너 default 서블릿이 처리한다.
 */
@WebServlet(name = "dispatcher", urlPatterns = {"/"})
public class DispatcherServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private final PostDao postDao = new PostDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 컨텍스트 경로를 제외한 실제 경로 (예: /posts/hello-world)
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path == null || path.isEmpty()) path = "/";

        req.setAttribute("baseUrl", SiteConfig.baseUrl());
        req.setAttribute("siteName", SiteConfig.siteName());

        try {
            if (path.equals("/")) {
                handleHome(req, resp);
            } else if (path.startsWith("/posts/")) {
                String slug = path.substring("/posts/".length());
                handlePostDetail(req, resp, slug);
            } else {
                notFound(req, resp);
            }
        } catch (RuntimeException e) {
            req.setAttribute("errorMessage", e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            forward(req, resp, "/WEB-INF/views/error.jsp");
        }
    }

    private void handleHome(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int page = parsePage(req.getParameter("page"));
        int offset = (page - 1) * PAGE_SIZE;

        List<Post> posts = postDao.findPublished(offset, PAGE_SIZE);
        int total = postDao.countPublished();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));

        req.setAttribute("posts", posts);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("pageTitle", SiteConfig.siteName());
        req.setAttribute("metaDescription", SiteConfig.siteDescription());
        req.setAttribute("canonical", SiteConfig.baseUrl() + "/"
                + (page > 1 ? "?page=" + page : ""));
        forward(req, resp, "/WEB-INF/views/post/list.jsp");
    }

    private void handlePostDetail(HttpServletRequest req, HttpServletResponse resp, String slug)
            throws ServletException, IOException {
        if (slug.isBlank()) { notFound(req, resp); return; }

        Post post = postDao.findBySlug(slug);
        if (post == null) { notFound(req, resp); return; }

        postDao.incrementViewCount(post.getId());

        req.setAttribute("post", post);
        req.setAttribute("pageTitle", post.getTitle() + " | " + SiteConfig.siteName());
        req.setAttribute("metaDescription",
                post.getMetaDescription() != null ? post.getMetaDescription() : post.getSummary());
        String url = SiteConfig.baseUrl() + "/posts/" + post.getSlug();
        req.setAttribute("canonical", url);
        req.setAttribute("jsonLd", buildJsonLd(post, url));
        forward(req, resp, "/WEB-INF/views/post/detail.jsp");
    }

    /** BlogPosting 구조화 데이터(JSON-LD)를 안전하게 문자열로 생성. */
    private String buildJsonLd(Post p, String url) {
        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String pub = p.getPublishedAt() != null ? p.getPublishedAt().format(iso) : "";
        String mod = p.getUpdatedAt()   != null ? p.getUpdatedAt().format(iso)   : pub;
        String desc = p.getMetaDescription() != null ? p.getMetaDescription()
                : (p.getSummary() != null ? p.getSummary() : "");
        String site = SiteConfig.siteName();

        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("\"@context\":\"https://schema.org\",")
          .append("\"@type\":\"BlogPosting\",")
          .append("\"headline\":\"").append(jsonEsc(p.getTitle())).append("\",")
          .append("\"description\":\"").append(jsonEsc(desc)).append("\",")
          .append("\"url\":\"").append(jsonEsc(url)).append("\",")
          .append("\"mainEntityOfPage\":\"").append(jsonEsc(url)).append("\",");
        if (!pub.isEmpty()) sb.append("\"datePublished\":\"").append(pub).append("\",");
        if (!mod.isEmpty()) sb.append("\"dateModified\":\"").append(mod).append("\",");
        if (p.getThumbnailUrl() != null && !p.getThumbnailUrl().isBlank())
            sb.append("\"image\":\"").append(jsonEsc(p.getThumbnailUrl())).append("\",");
        sb.append("\"author\":{\"@type\":\"Organization\",\"name\":\"").append(jsonEsc(site)).append("\"},")
          .append("\"publisher\":{\"@type\":\"Organization\",\"name\":\"").append(jsonEsc(site)).append("\"}")
          .append("}");
        return sb.toString();
    }

    private String jsonEsc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("</", "<\\/").replace("\n", " ").replace("\r", " ");
    }

    private void notFound(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        req.setAttribute("pageTitle", "404 Not Found | " + SiteConfig.siteName());
        forward(req, resp, "/WEB-INF/views/404.jsp");
    }

    private int parsePage(String raw) {
        try {
            int p = Integer.parseInt(raw);
            return Math.max(1, p);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void forward(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.getRequestDispatcher(view).forward(req, resp);
    }
}
