package com.example.blog.controller;

import com.example.blog.dao.CategoryDao;
import com.example.blog.dao.PostDao;
import com.example.blog.dao.TagDao;
import com.example.blog.model.Category;
import com.example.blog.model.Post;
import com.example.blog.model.Tag;
import com.example.blog.util.SiteConfig;
import com.example.blog.util.UrlUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 발행 글을 기반으로 sitemap.xml 을 동적으로 생성한다. (SEO)
 */
@WebServlet(urlPatterns = {"/sitemap.xml"})
public class SitemapServlet extends HttpServlet {

    private final PostDao postDao = new PostDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final TagDao tagDao = new TagDao();
    private static final DateTimeFormatter W3C = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/xml; charset=UTF-8");
        String base = SiteConfig.baseUrl();
        List<Post> posts = postDao.findAllPublishedForFeed(5000);

        try (PrintWriter out = resp.getWriter()) {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.println("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
            // 홈. lastmod 는 가장 최근 글의 수정 시각을 쓴다(목록이 그때 바뀌므로).
            String homeLastmod = posts.stream()
                    .map(Post::getUpdatedAt).filter(java.util.Objects::nonNull)
                    .max(java.time.LocalDateTime::compareTo)
                    .map(W3C::format).orElse(null);
            out.println("  <url><loc>" + esc(base) + "/</loc>"
                    + (homeLastmod != null ? "<lastmod>" + homeLastmod + "</lastmod>" : "")
                    + "<changefreq>daily</changefreq></url>");
            // 각 글
            for (Post p : posts) {
                out.println("  <url>");
                out.println("    <loc>" + esc(base + UrlUtil.encodePath("/posts/" + p.getSlug())) + "</loc>");
                if (p.getUpdatedAt() != null) {
                    out.println("    <lastmod>" + p.getUpdatedAt().format(W3C) + "</lastmod>");
                }
                out.println("    <changefreq>weekly</changefreq>");
                out.println("  </url>");
            }
            // 카테고리 / 태그 아카이브 (발행 글이 있는 것만)
            for (Category c : categoryDao.findAllWithPublishedPosts()) {
                out.println("  <url><loc>" + esc(base + UrlUtil.encodePath("/category/" + c.getSlug()))
                        + "</loc><changefreq>weekly</changefreq></url>");
            }
            for (Tag t : tagDao.findAllWithPublishedPosts()) {
                out.println("  <url><loc>" + esc(base + UrlUtil.encodePath("/tag/" + t.getSlug()))
                        + "</loc><changefreq>weekly</changefreq></url>");
            }
            out.println("</urlset>");
        }
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
