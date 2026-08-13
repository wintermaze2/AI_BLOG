package com.example.blog.controller;

import com.example.blog.dao.PostDao;
import com.example.blog.model.Post;
import com.example.blog.util.SiteConfig;

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
    private static final DateTimeFormatter W3C = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/xml; charset=UTF-8");
        String base = SiteConfig.baseUrl();
        List<Post> posts = postDao.findAllPublishedForFeed(5000);

        try (PrintWriter out = resp.getWriter()) {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.println("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
            // 홈
            out.println("  <url><loc>" + esc(base) + "/</loc><changefreq>daily</changefreq></url>");
            // 각 글
            for (Post p : posts) {
                out.println("  <url>");
                out.println("    <loc>" + esc(base + "/posts/" + p.getSlug()) + "</loc>");
                if (p.getUpdatedAt() != null) {
                    out.println("    <lastmod>" + p.getUpdatedAt().format(W3C) + "</lastmod>");
                }
                out.println("    <changefreq>weekly</changefreq>");
                out.println("  </url>");
            }
            out.println("</urlset>");
        }
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
