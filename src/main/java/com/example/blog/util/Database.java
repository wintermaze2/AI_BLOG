package com.example.blog.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * HikariCP 커넥션 풀 관리.
 *
 * 설정은 환경변수로 주입합니다(운영 권장). 미설정 시 기본값(localhost/blog/bloguser)을 사용합니다.
 *   BLOG_DB_URL   예) jdbc:mysql://127.0.0.1:3306/blog?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=utf8
 *   BLOG_DB_USER  예) bloguser
 *   BLOG_DB_PASS  예) (비밀번호)
 *
 * Tomcat systemd 서비스에서 Environment=... 로 넣거나, setenv.sh 에 export 하면 됩니다.
 */
public final class Database {

    private static volatile HikariDataSource dataSource;

    private Database() {}

    public static void init() {
        if (dataSource != null) return;
        synchronized (Database.class) {
            if (dataSource != null) return;

            String url  = env("BLOG_DB_URL",
                    "jdbc:mysql://127.0.0.1:3306/blog"
                    + "?useSSL=false&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=Asia/Seoul&characterEncoding=utf8");
            String user = env("BLOG_DB_USER", "bloguser");
            String pass = env("BLOG_DB_PASS", "change_me");

            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(url);
            cfg.setUsername(user);
            cfg.setPassword(pass);
            cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
            // t4g.small(2GB) 환경에 맞춘 보수적 풀 크기
            cfg.setMaximumPoolSize(10);
            cfg.setMinimumIdle(2);
            cfg.setPoolName("blog-pool");
            cfg.setConnectionTimeout(10_000);

            dataSource = new HikariDataSource(cfg);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) init();
        return dataSource.getConnection();
    }

    public static DataSource getDataSource() {
        if (dataSource == null) init();
        return dataSource;
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
