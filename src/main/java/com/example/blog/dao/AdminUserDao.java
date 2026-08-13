package com.example.blog.dao;

import com.example.blog.model.AdminUser;
import com.example.blog.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * admin_user 테이블 접근 DAO.
 */
public class AdminUserDao {

    public AdminUser findByUsername(String username) {
        String sql = "SELECT id, username, password_hash FROM admin_user WHERE username = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                AdminUser u = new AdminUser();
                u.setId(rs.getLong("id"));
                u.setUsername(rs.getString("username"));
                u.setPasswordHash(rs.getString("password_hash"));
                return u;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUsername 실패", e);
        }
    }
}
