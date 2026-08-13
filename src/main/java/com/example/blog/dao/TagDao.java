package com.example.blog.dao;

import com.example.blog.model.Tag;
import com.example.blog.util.Database;
import com.example.blog.util.SlugUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * tag / post_tag 테이블 접근 DAO. 모든 쿼리는 PreparedStatement로 SQL 인젝션을 방지한다.
 */
public class TagDao {

    /** DB 컬럼 길이 제한(schema.sql: name VARCHAR(80), slug VARCHAR(100)) */
    private static final int NAME_MAX = 80;
    private static final int SLUG_MAX = 100;
    /** 글 하나에 붙일 수 있는 태그 수 상한(폼 입력 남용 방지) */
    private static final int MAX_TAGS_PER_POST = 20;

    /** slug로 태그 1건 조회. 없으면 null. */
    public Tag findBySlug(String slug) {
        String sql = "SELECT id, name, slug FROM tag WHERE slug = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("tag findBySlug 실패", e);
        }
    }

    /** 특정 글에 붙은 태그 목록. */
    public List<Tag> findByPostId(long postId) {
        String sql = "SELECT t.id, t.name, t.slug FROM tag t " +
                     "JOIN post_tag pt ON pt.tag_id = t.id " +
                     "WHERE pt.post_id = ? ORDER BY t.name";
        List<Tag> list = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("tag findByPostId 실패", e);
        }
        return list;
    }

    /**
     * 발행 글이 1건 이상 달린 태그만 조회 (sitemap 용).
     * 빈 아카이브 페이지를 sitemap에 넣지 않기 위함.
     */
    public List<Tag> findAllWithPublishedPosts() {
        String sql = "SELECT DISTINCT t.id, t.name, t.slug FROM tag t " +
                     "JOIN post_tag pt ON pt.tag_id = t.id " +
                     "JOIN post p ON p.id = pt.post_id " +
                     "WHERE p.status = 'PUBLISHED' ORDER BY t.name";
        List<Tag> list = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("tag findAllWithPublishedPosts 실패", e);
        }
        return list;
    }

    /**
     * 글의 태그를 입력된 이름 목록과 일치하도록 갱신한다.
     * 기존 연결을 모두 지우고 다시 연결하므로, 태그를 제거한 경우도 반영된다.
     * 여러 문장을 쓰므로 하나의 트랜잭션으로 묶는다.
     *
     * @param names 태그 이름 목록(정규화 전 원문). 비어 있으면 연결만 모두 제거.
     */
    public void syncPostTags(long postId, List<String> names) {
        // slug 기준 중복 제거 (예: "Java"와 "java"는 같은 태그)
        Map<String, String> slugToName = new LinkedHashMap<>();
        if (names != null) {
            for (String raw : names) {
                if (raw == null || raw.isBlank()) continue;
                String name = clip(raw.trim(), NAME_MAX);
                String slug = clip(SlugUtil.toSlug(name), SLUG_MAX);
                if (slug.isBlank()) continue;
                slugToName.putIfAbsent(slug, name);
                if (slugToName.size() >= MAX_TAGS_PER_POST) break;
            }
        }

        Connection con = null;
        try {
            con = Database.getConnection();
            con.setAutoCommit(false);

            // 1) 기존 연결 제거
            try (PreparedStatement ps =
                         con.prepareStatement("DELETE FROM post_tag WHERE post_id = ?")) {
                ps.setLong(1, postId);
                ps.executeUpdate();
            }

            // 2) 태그를 찾거나 새로 만들고 다시 연결
            for (Map.Entry<String, String> e : slugToName.entrySet()) {
                long tagId = findOrCreateTagId(con, e.getKey(), e.getValue());
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO post_tag (post_id, tag_id) VALUES (?, ?)")) {
                    ps.setLong(1, postId);
                    ps.setLong(2, tagId);
                    ps.executeUpdate();
                }
            }

            // 3) 이번 변경으로 어느 글에도 붙어있지 않게 된 태그 정리
            deleteOrphans(con);

            con.commit();
        } catch (SQLException e) {
            rollbackQuietly(con);
            throw new RuntimeException("syncPostTags 실패", e);
        } finally {
            closeQuietly(con);
        }
    }

    /**
     * 어느 글에도 연결되지 않은 태그를 삭제한다.
     * 글 삭제(post_tag는 CASCADE로 정리됨) 직후에도 호출해 고아 태그가 남지 않게 한다.
     *
     * @return 삭제된 태그 수
     */
    public int deleteOrphans() {
        try (Connection con = Database.getConnection()) {
            return deleteOrphans(con);
        } catch (SQLException e) {
            throw new RuntimeException("deleteOrphans 실패", e);
        }
    }

    /** 호출자가 트랜잭션을 관리하는 경우용. */
    private int deleteOrphans(Connection con) throws SQLException {
        String sql = "DELETE t FROM tag t " +
                     "LEFT JOIN post_tag pt ON pt.tag_id = t.id " +
                     "WHERE pt.tag_id IS NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    /** 트랜잭션 안에서 slug로 태그를 찾고, 없으면 생성해 id를 반환. */
    private long findOrCreateTagId(Connection con, String slug, String name) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM tag WHERE slug = ?")) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO tag (name, slug) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, slug);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("태그 생성 후 id를 가져오지 못했습니다: " + slug);
    }

    /** "java, spring , java" -> ["java", "spring", "java"] (중복 제거는 syncPostTags에서) */
    public static List<String> parseNames(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;
        for (String part : raw.split("[,\\n]")) {
            String s = part.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static String clip(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) : s;
    }

    private void rollbackQuietly(Connection con) {
        if (con == null) return;
        try { con.rollback(); } catch (SQLException ignore) { /* 원래 예외를 가리지 않는다 */ }
    }

    private void closeQuietly(Connection con) {
        if (con == null) return;
        // 풀로 반납하기 전에 autoCommit 원복
        try { con.setAutoCommit(true); } catch (SQLException ignore) { }
        try { con.close(); } catch (SQLException ignore) { }
    }

    private Tag map(ResultSet rs) throws SQLException {
        Tag t = new Tag();
        t.setId(rs.getLong("id"));
        t.setName(rs.getString("name"));
        t.setSlug(rs.getString("slug"));
        return t;
    }
}
