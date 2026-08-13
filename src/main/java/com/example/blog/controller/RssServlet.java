package com.example.blog.controller;

import com.example.blog.dao.PostDao;
import com.example.blog.model.Post;
import com.example.blog.util.SiteConfig;
import com.example.blog.util.UrlUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 최신 발행 글 RSS 2.0 피드. (/rss.xml)
 */
@WebServlet(urlPatterns = {"/rss.xml"})
public class RssServlet extends HttpServlet {

    private final PostDao postDao = new PostDao();
    private static final DateTimeFormatter RFC822 =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/rss+xml; charset=UTF-8");
        String base = SiteConfig.baseUrl();
        List<Post> posts = postDao.findAllPublishedForFeed(30);

        try (PrintWriter out = resp.getWriter()) {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.println("<rss version=\"2.0\"><channel>");
            out.println("  <title>" + esc(SiteConfig.siteName()) + "</title>");
            out.println("  <link>" + esc(base) + "/</link>");
            out.println("  <description>" + esc(SiteConfig.siteDescription()) + "</description>");
            for (Post p : posts) {
                String link = base + UrlUtil.encodePath("/posts/" + p.getSlug());
                out.println("  <item>");
                out.println("    <title>" + esc(p.getTitle()) + "</title>");
                out.println("    <link>" + esc(link) + "</link>");
                out.println("    <guid>" + esc(link) + "</guid>");
                if (p.getSummary() != null) {
                    out.println("    <description>" + esc(p.getSummary()) + "</description>");
                }
                if (p.getPublishedAt() != null) {
                    ZonedDateTime z = p.getPublishedAt().atZone(SEOUL);
                    out.println("    <pubDate>" + z.format(RFC822) + "</pubDate>");
                }
                out.println("  </item>");
            }
            out.println("</channel></rss>");
        }
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
