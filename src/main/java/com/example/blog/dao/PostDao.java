package com.example.blog.dao;

import com.example.blog.model.Post;
import com.example.blog.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * post 테이블 접근 DAO. 모든 쿼리는 PreparedStatement로 SQL 인젝션을 방지한다.
 */
public class PostDao {

    private static final String BASE_SELECT =
            "SELECT p.id, p.slug, p.title, p.summary, p.content_html, p.thumbnail_url, " +
            "       p.meta_description, p.status, p.view_count, p.category_id, " +
            "       p.created_at, p.updated_at, p.published_at, " +
            "       c.name AS category_name, c.slug AS category_slug " +
            "FROM post p LEFT JOIN category c ON p.category_id = c.id ";

    /** 발행된 글 목록(페이징). */
    public List<Post> findPublished(int offset, int limit) {
        String sql = BASE_SELECT +
                "WHERE p.status = 'PUBLISHED' " +
                "ORDER BY p.published_at DESC " +
                "LIMIT ? OFFSET ?";
        List<Post> list = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findPublished 실패", e);
        }
        return list;
    }

    /** 발행 글 총 개수(페이지네이션용). */
    public int countPublished() {
        String sql = "SELECT COUNT(*) FROM post WHERE status = 'PUBLISHED'";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("countPublished 실패", e);
        }
    }

    /** slug로 발행 글 1건 조회. 없으면 null. */
    public Post findBySlug(String slug) {
        String sql = BASE_SELECT + "WHERE p.slug = ? AND p.status = 'PUBLISHED'";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findBySlug 실패", e);
        }
    }

    /** sitemap / RSS 용: 발행 글 전체(간소 필드). */
    public List<Post> findAllPublishedForFeed(int limit) {
        String sql = BASE_SELECT +
                "WHERE p.status = 'PUBLISHED' ORDER BY p.published_at DESC LIMIT ?";
        List<Post> list = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAllPublishedForFeed 실패", e);
        }
        return list;
    }

    /** 조회수 +1 (비동기적으로 호출해도 무방). */
    public void incrementViewCount(long id) {
        String sql = "UPDATE post SET view_count = view_count + 1 WHERE id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            // 조회수는 부가 기능이므로 실패해도 페이지 표시엔 영향 없게 로그만
            System.err.println("incrementViewCount 실패: " + e.getMessage());
        }
    }

    // =================================================================
    // 관리자용 CRUD
    // =================================================================

    /** 관리자 목록: 모든 상태(DRAFT 포함), 최신 작성순. */
    public List<Post> findAllForAdmin(int offset, int limit) {
        String sql = BASE_SELECT +
                "ORDER BY p.created_at DESC LIMIT ? OFFSET ?";
        List<Post> list = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAllForAdmin 실패", e);
        }
        return list;
    }

    /** 전체 글 개수(상태 무관). */
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM post";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("countAll 실패", e);
        }
    }

    /** 편집용 단건 조회(content_md 포함). 없으면 null. */
    public Post findByIdFull(long id) {
        String sql =
                "SELECT p.id, p.slug, p.title, p.summary, p.content_md, p.content_html, " +
                "       p.thumbnail_url, p.meta_description, p.status, p.view_count, p.category_id, " +
                "       p.created_at, p.updated_at, p.published_at, " +
                "       c.name AS category_name, c.slug AS category_slug " +
                "FROM post p LEFT JOIN category c ON p.category_id = c.id WHERE p.id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Post p = map(rs);
                p.setContentMd(rs.getString("content_md"));
                return p;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByIdFull 실패", e);
        }
    }

    /** slug 중복 여부(자기 자신 제외). */
    public boolean slugExists(String slug, long excludeId) {
        String sql = "SELECT 1 FROM post WHERE slug = ? AND id <> ? LIMIT 1";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, slug);
            ps.setLong(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("slugExists 실패", e);
        }
    }

    /** 신규 글 저장. 생성된 id 반환. */
    public long insert(Post p) {
        String sql = "INSERT INTO post " +
                "(slug, title, summary, content_md, content_html, thumbnail_url, " +
                " meta_description, status, category_id, published_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindWrite(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert 실패", e);
        }
    }

    /** 기존 글 수정. */
    public void update(Post p) {
        String sql = "UPDATE post SET " +
                "slug=?, title=?, summary=?, content_md=?, content_html=?, thumbnail_url=?, " +
                "meta_description=?, status=?, category_id=?, published_at=? WHERE id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindWrite(ps, p);
            ps.setLong(11, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update 실패", e);
        }
    }

    /** 글 삭제. */
    public void delete(long id) {
        String sql = "DELETE FROM post WHERE id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete 실패", e);
        }
    }

    /** insert/update 공통 파라미터 바인딩(1~10). */
    private void bindWrite(PreparedStatement ps, Post p) throws SQLException {
        ps.setString(1, p.getSlug());
        ps.setString(2, p.getTitle());
        ps.setString(3, p.getSummary());
        ps.setString(4, p.getContentMd());
        ps.setString(5, p.getContentHtml());
        ps.setString(6, p.getThumbnailUrl());
        ps.setString(7, p.getMetaDescription());
        ps.setString(8, p.getStatus());
        if (p.getCategoryId() != null) ps.setLong(9, p.getCategoryId());
        else ps.setNull(9, java.sql.Types.BIGINT);
        if (p.getPublishedAt() != null) ps.setTimestamp(10, Timestamp.valueOf(p.getPublishedAt()));
        else ps.setNull(10, java.sql.Types.TIMESTAMP);
    }

    private Post map(ResultSet rs) throws SQLException {
        Post p = new Post();
        p.setId(rs.getLong("id"));
        p.setSlug(rs.getString("slug"));
        p.setTitle(rs.getString("title"));
        p.setSummary(rs.getString("summary"));
        p.setContentHtml(rs.getString("content_html"));
        p.setThumbnailUrl(rs.getString("thumbnail_url"));
        p.setMetaDescription(rs.getString("meta_description"));
        p.setStatus(rs.getString("status"));
        p.setViewCount(rs.getLong("view_count"));
        long catId = rs.getLong("category_id");
        if (!rs.wasNull()) p.setCategoryId(catId);
        p.setCategoryName(rs.getString("category_name"));
        p.setCategorySlug(rs.getString("category_slug"));
        p.setCreatedAt(toLdt(rs.getTimestamp("created_at")));
        p.setUpdatedAt(toLdt(rs.getTimestamp("updated_at")));
        p.setPublishedAt(toLdt(rs.getTimestamp("published_at")));
        return p;
    }

    private LocalDateTime toLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
