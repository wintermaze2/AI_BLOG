-- =====================================================================
-- 경량 블로그 스키마 (MySQL 8.4, utf8mb4)
-- 사용법: mysql -u bloguser -p blog < schema.sql
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 카테고리
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    slug        VARCHAR(120)  NOT NULL UNIQUE,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 글(post)
--   content_md  : 원문(편집용). content_type이 MD면 Markdown, HTML이면 HTML 소스
--   content_html: 렌더링된 HTML   - 조회 성능용(저장 시 미리 변환)
--   content_type: MD | HTML - 원문을 저장 시 변환할지(MD) 그대로 쓸지(HTML) 결정
--   status      : DRAFT | PUBLISHED
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS post (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug             VARCHAR(200)  NOT NULL UNIQUE,
    title            VARCHAR(300)  NOT NULL,
    summary          VARCHAR(500)      NULL,
    content_md       MEDIUMTEXT        NULL,
    content_html     MEDIUMTEXT        NULL,
    content_type     VARCHAR(10)   NOT NULL DEFAULT 'MD',
    thumbnail_url    VARCHAR(500)      NULL,
    meta_description VARCHAR(320)      NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    view_count       BIGINT        NOT NULL DEFAULT 0,
    category_id      BIGINT            NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    published_at     DATETIME          NULL,
    CONSTRAINT fk_post_category FOREIGN KEY (category_id)
        REFERENCES category(id) ON DELETE SET NULL,
    INDEX idx_post_status_published (status, published_at),
    INDEX idx_post_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 태그 & 글-태그 연결(다대다)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tag (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(80)  NOT NULL,
    slug  VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_tag (
    post_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    CONSTRAINT fk_pt_post FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    CONSTRAINT fk_pt_tag  FOREIGN KEY (tag_id)  REFERENCES tag(id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 관리자 계정 (비밀번호는 BCrypt 해시 저장)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 마이그레이션 (이미 만들어진 DB에 나중에 추가된 컬럼 반영)
--
-- CREATE TABLE IF NOT EXISTS 는 기존 테이블에 컬럼을 추가해 주지 않으므로,
-- 운영 중인 DB를 위해 여기서 따로 처리한다.
-- MySQL 8.4 에는 ADD COLUMN IF NOT EXISTS 가 없어서, information_schema 를 보고
-- 필요할 때만 ALTER 를 실행하도록 만들었다. 이 파일을 여러 번 실행해도 안전하다.
-- =====================================================================

-- post.content_type (본문 형식: MD | HTML)
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'post'
       AND COLUMN_NAME  = 'content_type'
);
SET @ddl := IF(@col_exists > 0,
    'SELECT ''post.content_type 컬럼이 이미 있습니다 - 건너뜀'' AS migration',
    'ALTER TABLE post ADD COLUMN content_type VARCHAR(10) NOT NULL DEFAULT ''MD'' AFTER content_html'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 샘플 데이터
-- =====================================================================
INSERT INTO category (name, slug) VALUES
    ('개발', 'dev'),
    ('일상', 'life')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO post (slug, title, summary, content_md, content_html, meta_description, status, category_id, published_at)
VALUES
(
    'hello-world',
    '첫 번째 글: 블로그를 시작하며',
    '자체 구축한 경량 JSP 블로그의 첫 글입니다.',
    '# 안녕하세요\n\n이 블로그는 **Tomcat + JSP + MySQL**로 직접 만들었습니다.\n\n- SEO 태그 자유 제어\n- JSON-LD 구조화 데이터\n- 동적 sitemap / RSS',
    '<h1>안녕하세요</h1><p>이 블로그는 <strong>Tomcat + JSP + MySQL</strong>로 직접 만들었습니다.</p><ul><li>SEO 태그 자유 제어</li><li>JSON-LD 구조화 데이터</li><li>동적 sitemap / RSS</li></ul>',
    '자체 구축한 경량 JSP 블로그의 첫 글 - Tomcat, JSP, MySQL 기반',
    'PUBLISHED',
    1,
    NOW()
),
(
    'second-post',
    'Markdown으로 글쓰기',
    '이 블로그는 Markdown 원문을 HTML로 변환해 저장합니다.',
    '## Markdown 지원\n\n코드 블록도 됩니다.\n\n```java\nSystem.out.println("Hello, Blog!");\n```',
    '<h2>Markdown 지원</h2><p>코드 블록도 됩니다.</p><pre><code class="language-java">System.out.println("Hello, Blog!");</code></pre>',
    'Markdown으로 글을 작성하고 HTML로 변환해 저장하는 방법',
    'PUBLISHED',
    1,
    NOW()
)
ON DUPLICATE KEY UPDATE title = VALUES(title);
