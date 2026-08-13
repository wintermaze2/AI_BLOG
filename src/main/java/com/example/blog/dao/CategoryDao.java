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
            while (rs.next()) {
                Category c = new Category();
                c.setId(rs.getLong("id"));
                c.setName(rs.getString("name"));
                c.setSlug(rs.getString("slug"));
                list.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException("category findAll 실패", e);
        }
        return list;
    }
}
