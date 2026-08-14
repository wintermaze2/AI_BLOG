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
            "SELECT p.id, p.slug, p.title, p.summary, p.content_html, p.content_type, " +
            "       p.thumbnail_url, " +
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

    /** 카테고리별 발행 글 목록(페이징). */
    public List<Post> findPublishedByCategory(String categorySlug, int offset, int limit) {
        String sql = BASE_SELECT +
                "WHERE p.status = 'PUBLISHED' AND c.slug = ? " +
                "ORDER BY p.published_at DESC " +
                "LIMIT ? OFFSET ?";
        return querySlugPaged(sql, categorySlug, offset, limit, "findPublishedByCategory");
    }

    /** 카테고리별 발행 글 수. */
    public int countPublishedByCategory(String categorySlug) {
        String sql = "SELECT COUNT(*) FROM post p JOIN category c ON p.category_id = c.id " +
                     "WHERE p.status = 'PUBLISHED' AND c.slug = ?";
        return countBySlug(sql, categorySlug, "countPublishedByCategory");
    }

    /** 태그별 발행 글 목록(페이징). */
    public List<Post> findPublishedByTag(String tagSlug, int offset, int limit) {
        String sql = BASE_SELECT +
                "JOIN post_tag pt ON pt.post_id = p.id " +
                "JOIN tag t ON t.id = pt.tag_id " +
                "WHERE p.status = 'PUBLISHED' AND t.slug = ? " +
                "ORDER BY p.published_at DESC " +
                "LIMIT ? OFFSET ?";
        return querySlugPaged(sql, tagSlug, offset, limit, "findPublishedByTag");
    }

    /** 태그별 발행 글 수. */
    public int countPublishedByTag(String tagSlug) {
        String sql = "SELECT COUNT(*) FROM post p " +
                     "JOIN post_tag pt ON pt.post_id = p.id " +
                     "JOIN tag t ON t.id = pt.tag_id " +
                     "WHERE p.status = 'PUBLISHED' AND t.slug = ?";
        return countBySlug(sql, tagSlug, "countPublishedByTag");
    }

    // =================================================================
    // 검색
    // =================================================================

    /** 제목/요약/본문(Markdown 원문)을 대상으로 하는 부분 일치 검색 조건. */
    private static final String SEARCH_WHERE =
            "WHERE p.status = 'PUBLISHED' AND (" +
            "      p.title      LIKE ? ESCAPE '\\\\' " +
            "   OR p.summary    LIKE ? ESCAPE '\\\\' " +
            "   OR p.content_md LIKE ? ESCAPE '\\\\') ";

    /**
     * 발행 글 검색(페이징). 제목이 일치하는 글을 먼저 보여주고, 그다음 최신순.
     *
     * 규모가 커지면 LIKE '%..%' 는 인덱스를 못 타므로 FULLTEXT(ngram) 전환을 검토할 것.
     * 현재 글 수 기준에서는 단순 LIKE로 충분하다.
     */
    public List<Post> searchPublished(String query, int offset, int limit) {
        String sql = BASE_SELECT + SEARCH_WHERE +
                "ORDER BY (p.title LIKE ? ESCAPE '\\\\') DESC, p.published_at DESC " +
                "LIMIT ? OFFSET ?";
        String pattern = likePattern(query);
        List<Post> list = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern);   // title
            ps.setString(2, pattern);   // summary
            ps.setString(3, pattern);   // content_md
            ps.setString(4, pattern);   // ORDER BY 제목 우선
            ps.setInt(5, limit);
            ps.setInt(6, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("searchPublished 실패", e);
        }
        return list;
    }

    /** 검색 결과 총 개수. */
    public int countSearchPublished(String query) {
        String sql = "SELECT COUNT(*) FROM post p " + SEARCH_WHERE;
        String pattern = likePattern(query);
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("countSearchPublished 실패", e);
        }
    }

    /**
     * 사용자 입력을 LIKE 패턴으로 감싼다.
     * 값 자체는 PreparedStatement로 바인딩되지만, 입력에 포함된 '%'와 '_'는
     * 그대로 두면 와일드카드로 동작하므로 이스케이프해야 한다.
     */
    private String likePattern(String query) {
        String escaped = (query == null ? "" : query)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /** 아카이브 목록 공통: (slug, limit, offset) 바인딩 후 매핑. */
    private List<Post> querySlugPaged(String sql, String slug, int offset, int limit, String label) {
        List<Post> list = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, slug);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(label + " 실패", e);
        }
        return list;
    }

    /** 아카이브 개수 공통: (slug) 바인딩 후 COUNT 반환. */
    private int countBySlug(String sql, String slug, String label) {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(label + " 실패", e);
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
                "       p.content_type, " +
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
                " meta_description, status, category_id, published_at, content_type) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
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
                "meta_description=?, status=?, category_id=?, published_at=?, content_type=? " +
                "WHERE id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindWrite(ps, p);
            ps.setLong(12, p.getId());
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

    /** insert/update 공통 파라미터 바인딩(1~11). update는 이어서 12번에 id를 바인딩한다. */
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
        ps.setString(11, p.getContentType());
    }

    private Post map(ResultSet rs) throws SQLException {
        Post p = new Post();
        p.setId(rs.getLong("id"));
        p.setSlug(rs.getString("slug"));
        p.setTitle(rs.getString("title"));
        p.setSummary(rs.getString("summary"));
        p.setContentHtml(rs.getString("content_html"));
        p.setContentType(rs.getString("content_type"));
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
