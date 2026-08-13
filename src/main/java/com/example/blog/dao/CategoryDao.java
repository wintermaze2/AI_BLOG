package com.example.blog.dao;

import com.example.blog.model.Category;
import com.example.blog.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    public List<Category> findAll() {
        String sql = "SELECT id, name, slug FROM category ORDER BY name";
        List<Category> list = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("category findAll 실패", e);
        }
        return list;
    }

    /** slug로 카테고리 1건 조회. 없으면 null. */
    public Category findBySlug(String slug) {
        String sql = "SELECT id, name, slug FROM category WHERE slug = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("category findBySlug 실패", e);
        }
    }

    /**
     * 발행 글이 1건 이상 있는 카테고리만 조회 (sitemap 용).
     * 빈 아카이브 페이지를 sitemap에 넣지 않기 위함.
     */
    public List<Category> findAllWithPublishedPosts() {
        String sql = "SELECT DISTINCT c.id, c.name, c.slug FROM category c " +
                     "JOIN post p ON p.category_id = c.id " +
                     "WHERE p.status = 'PUBLISHED' ORDER BY c.name";
        List<Category> list = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("category findAllWithPublishedPosts 실패", e);
        }
        return list;
    }

    private Category map(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setSlug(rs.getString("slug"));
        return c;
    }
}
