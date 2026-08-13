# 경량 JSP 블로그 스켈레톤

Tomcat 11 + JSP(Jakarta) + MySQL 8.4 환경을 위한 SEO 친화 블로그 뼈대입니다.

## 특징
- **프론트 컨트롤러(MVC)** — `DispatcherServlet` 이 예쁜 URL을 라우팅
  - `/` 글 목록(페이징), `/posts/{slug}` 글 상세
- **SEO** — 글마다 title/description/canonical, Open Graph, **JSON-LD(BlogPosting)**
- **동적 `sitemap.xml` / `rss.xml`** — 발행 글 기반 자동 생성
- **Markdown → HTML** — flexmark, 저장 시점 변환 저장(조회 성능)
- **HikariCP** 커넥션 풀, **PreparedStatement** 로 SQL 인젝션 방지
- **Jakarta 네임스페이스** — `jakarta.servlet.*`, JSTL 3.0(`jakarta.tags.core`)

## 요구사항
- JDK 21, Maven 3.9+, MySQL 8.4, Tomcat 11

## 1. DB 준비
```bash
mysql -u bloguser -p blog < sql/schema.sql
```

## 2. 접속 정보 설정 (환경변수 권장)
Tomcat 서비스에 아래 환경변수를 넣습니다. `/etc/systemd/system/tomcat.service` 의
`[Service]` 섹션에 추가 후 `systemctl daemon-reload && systemctl restart tomcat`:
```
Environment="BLOG_DB_URL=jdbc:mysql://127.0.0.1:3306/blog?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=utf8"
Environment="BLOG_DB_USER=bloguser"
Environment="BLOG_DB_PASS=실제_비밀번호"
Environment="BLOG_BASE_URL=https://your-domain.com"
Environment="BLOG_SITE_NAME=내 블로그"
```
미설정 시 `Database.java` / `SiteConfig.java` 의 기본값을 사용합니다.

## 3. 빌드
```bash
mvn clean package
# 결과물: target/blog.war
```

## 4. 배포
도메인 루트(`https://도메인/`)에서 서비스하려면 **ROOT 로 배포**합니다:
```bash
sudo systemctl stop tomcat
sudo rm -rf /opt/tomcat/webapps/ROOT
sudo cp target/blog.war /opt/tomcat/webapps/ROOT.war
sudo systemctl start tomcat
```
(하위 경로 `/blog` 로 두려면 `blog.war` 그대로 복사)

## 5. 확인
- `https://도메인/` 목록
- `https://도메인/posts/hello-world` 상세 (페이지 소스에서 JSON-LD 확인)
- `https://도메인/sitemap.xml`, `https://도메인/rss.xml`

## 디렉터리 구조
```
blog/
├── pom.xml
├── sql/schema.sql
└── src/main/
    ├── java/com/example/blog/
    │   ├── controller/  (DispatcherServlet, SitemapServlet, RssServlet)
    │   ├── dao/         (PostDao)
    │   ├── model/       (Post)
    │   └── util/        (Database, MarkdownUtil, SlugUtil, SiteConfig, AppContextListener)
    └── webapp/
        ├── WEB-INF/web.xml
        ├── WEB-INF/views/ (layout, post/list, post/detail, 404, error)
        ├── static/css/style.css
        └── robots.txt
```

## 관리자 기능 (글쓰기)

세션 기반 로그인 후 `/admin` 에서 글을 작성/수정/삭제할 수 있습니다. 본문은 Markdown 에디터(EasyMDE)로 작성하며, 저장 시 HTML로 변환되어 저장됩니다.

### 최초 관리자 계정 만들기
1. 비밀번호의 BCrypt 해시를 생성합니다(프로젝트 폴더에서):
   ```bash
   mvn -q compile exec:java \
       -Dexec.mainClass=com.example.blog.tool.GenerateHash \
       -Dexec.args="원하는_비밀번호"
   ```
   출력된 `$2a$...` 해시를 복사합니다.
2. DB에 관리자 계정을 INSERT 합니다:
   ```sql
   INSERT INTO admin_user (username, password_hash)
   VALUES ('admin', '<위에서_출력된_해시>');
   ```
3. `https://도메인/admin/login` 에서 로그인합니다.

### 경로
- `/admin/login` 로그인 · `/admin` 글 목록(임시글 포함)
- `/admin/new` 새 글 · `/admin/edit?id=..` 수정 · `/admin/logout` 로그아웃

### 보안 참고
- `/admin/*` 는 `AuthFilter` 로 보호되며 미로그인 시 로그인 페이지로 이동합니다.
- 저장/삭제는 세션 CSRF 토큰으로 보호됩니다.
- 관리자 페이지는 `noindex` 및 `robots.txt` Disallow 로 검색 제외됩니다.
- 비밀번호는 BCrypt(cost 12)로 저장됩니다.

## 다음 확장 아이디어
- 카테고리/태그별 목록 라우팅
- 댓글, 조회수 상위 위젯, 검색
- 정적 리소스 캐시 헤더 / gzip (Nginx 단에서)
