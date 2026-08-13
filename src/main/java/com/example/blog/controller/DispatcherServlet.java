package com.example.blog.controller;

import com.example.blog.dao.CategoryDao;
import com.example.blog.dao.PostDao;
import com.example.blog.dao.TagDao;
import com.example.blog.model.Category;
import com.example.blog.model.Post;
import com.example.blog.model.Tag;
import com.example.blog.util.SiteConfig;
import com.example.blog.util.UrlUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 프론트 컨트롤러: 모든 요청(정적 리소스 제외)을 받아 예쁜 URL을 분석해 라우팅한다.
 *
 *   /                     -> 글 목록(홈), ?page=N 페이징
 *   /posts/{slug}         -> 글 상세
 *   /category/{slug}      -> 카테고리별 목록, ?page=N 페이징
 *   /tag/{slug}           -> 태그별 목록, ?page=N 페이징
 *   /search?q=...         -> 검색 결과, ?page=N 페이징
 *   그 외                  -> 404
 *
 * 정적 리소스(/static/*)는 web.xml에서 컨테이너 default 서블릿이 처리한다.
 */
@WebServlet(name = "dispatcher", urlPatterns = {"/"})
public class DispatcherServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    /** 검색어 길이 상한(과도한 입력 방지) */
    private static final int MAX_QUERY_LENGTH = 100;
    private final PostDao postDao = new PostDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final TagDao tagDao = new TagDao();

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
                handlePostDetail(req, resp, decodeSlug(path.substring("/posts/".length())));
            } else if (path.startsWith("/category/")) {
                handleCategory(req, resp, decodeSlug(path.substring("/category/".length())));
            } else if (path.startsWith("/tag/")) {
                handleTag(req, resp, decodeSlug(path.substring("/tag/".length())));
            } else if (path.equals("/search")) {
                handleSearch(req, resp);
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

        renderList(req, resp, page,
                postDao.findPublished(offset, PAGE_SIZE),
                postDao.countPublished(),
                SiteConfig.siteName(),                 // h1
                SiteConfig.siteName(),                 // <title>
                SiteConfig.siteDescription(),
                "/", null);
    }

    /**
     * /search?q=... — 발행 글 검색.
     * 검색 결과 페이지는 색인 대상이 아니므로(얇은 중복 콘텐츠) noindex 처리하고
     * canonical도 넣지 않는다.
     */
    private void handleSearch(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String q = req.getParameter("q");
        q = (q == null) ? "" : q.trim();
        if (q.length() > MAX_QUERY_LENGTH) q = q.substring(0, MAX_QUERY_LENGTH);

        int page = parsePage(req.getParameter("page"));
        int offset = (page - 1) * PAGE_SIZE;

        List<Post> posts = List.of();
        int total = 0;
        if (!q.isEmpty()) {
            posts = postDao.searchPublished(q, offset, PAGE_SIZE);
            total = postDao.countSearchPublished(q);
        }

        req.setAttribute("archiveKind", "search");
        req.setAttribute("query", q);
        req.setAttribute("noindex", true);

        String label = q.isEmpty() ? "검색" : "'" + q + "' 검색 결과";
        renderList(req, resp, page, posts, total,
                label,
                label + " | " + SiteConfig.siteName(),
                null,                                        // 검색 결과엔 meta description 불필요
                null,                                        // canonical 없음
                q.isEmpty() ? null : "q=" + UrlUtil.encodeQueryValue(q));
    }

    /** /category/{slug} — 카테고리 아카이브. */
    private void handleCategory(HttpServletRequest req, HttpServletResponse resp, String slug)
            throws ServletException, IOException {
        if (slug.isBlank()) { notFound(req, resp); return; }

        Category category = categoryDao.findBySlug(slug);
        if (category == null) { notFound(req, resp); return; }

        int page = parsePage(req.getParameter("page"));
        int offset = (page - 1) * PAGE_SIZE;

        req.setAttribute("archiveKind", "category");
        renderList(req, resp, page,
                postDao.findPublishedByCategory(slug, offset, PAGE_SIZE),
                postDao.countPublishedByCategory(slug),
                category.getName(),
                category.getName() + " | " + SiteConfig.siteName(),
                "'" + category.getName() + "' 카테고리의 글 목록입니다.",
                "/category/" + category.getSlug(), null);
    }

    /** /tag/{slug} — 태그 아카이브. */
    private void handleTag(HttpServletRequest req, HttpServletResponse resp, String slug)
            throws ServletException, IOException {
        if (slug.isBlank()) { notFound(req, resp); return; }

        Tag tag = tagDao.findBySlug(slug);
        if (tag == null) { notFound(req, resp); return; }

        int page = parsePage(req.getParameter("page"));
        int offset = (page - 1) * PAGE_SIZE;

        req.setAttribute("archiveKind", "tag");
        renderList(req, resp, page,
                postDao.findPublishedByTag(slug, offset, PAGE_SIZE),
                postDao.countPublishedByTag(slug),
                "#" + tag.getName(),
                tag.getName() + " 태그 | " + SiteConfig.siteName(),
                "'" + tag.getName() + "' 태그가 붙은 글 목록입니다.",
                "/tag/" + tag.getSlug(), null);
    }

    /**
     * 홈·카테고리·태그·검색이 공유하는 목록 렌더링.
     *
     * @param path       canonical 계산용 경로(예: "/", "/category/dev"). null이면 canonical을 넣지 않는다.
     *                   slug는 인코딩하지 않은 원문을 넘긴다(여기서 인코딩).
     * @param extraQuery 페이지 링크에 함께 유지할 쿼리 스트링(예: "q=tomcat"). 없으면 null.
     */
    private void renderList(HttpServletRequest req, HttpServletResponse resp,
                            int page, List<Post> posts, int total,
                            String heading, String pageTitle, String metaDescription,
                            String path, String extraQuery)
            throws ServletException, IOException {

        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));

        // 페이지 이동 링크의 접두사. 검색처럼 유지해야 할 파라미터가 있으면 함께 싣는다.
        String pageLinkBase = (extraQuery == null || extraQuery.isEmpty())
                ? "?page=" : "?" + extraQuery + "&page=";

        req.setAttribute("posts", posts);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("totalCount", total);
        req.setAttribute("heading", heading);
        req.setAttribute("pageTitle", pageTitle);
        req.setAttribute("metaDescription", metaDescription);
        req.setAttribute("pageLinkBase", pageLinkBase);
        if (path != null) {
            req.setAttribute("canonical", SiteConfig.baseUrl() + UrlUtil.encodePath(path)
                    + (page > 1 ? pageLinkBase + page : ""));
        }
        // 결과가 없는 아카이브는 얇은 콘텐츠이므로 색인시키지 않는다.
        if (total == 0 && req.getAttribute("archiveKind") != null) {
            req.setAttribute("noindex", true);
        }
        forward(req, resp, "/WEB-INF/views/post/list.jsp");
    }

    private void handlePostDetail(HttpServletRequest req, HttpServletResponse resp, String slug)
            throws ServletException, IOException {
        if (slug.isBlank()) { notFound(req, resp); return; }

        Post post = postDao.findBySlug(slug);
        if (post == null) { notFound(req, resp); return; }

        postDao.incrementViewCount(post.getId());
        post.setTags(tagDao.findByPostId(post.getId()));

        req.setAttribute("post", post);
        req.setAttribute("pageTitle", post.getTitle() + " | " + SiteConfig.siteName());
        req.setAttribute("metaDescription",
                post.getMetaDescription() != null ? post.getMetaDescription() : post.getSummary());
        String url = SiteConfig.baseUrl() + UrlUtil.encodePath("/posts/" + post.getSlug());
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

    /**
     * URL 경로 세그먼트를 디코딩한다.
     * getRequestURI()는 인코딩된 원문을 돌려주므로, 한글 slug는 여기서 풀어야 DB 값과 일치한다.
     * (SlugUtil이 '+'를 제거하므로 URLDecoder의 '+' -> 공백 변환은 실제 slug에 영향 없음)
     */
    private String decodeSlug(String raw) {
        if (raw == null) return "";
        String s = raw;
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);   // 끝의 슬래시 허용
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";   // 깨진 퍼센트 인코딩 -> 404로 흘려보낸다
        }
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
