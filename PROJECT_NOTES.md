# PROJECT NOTES — 경량 JSP 블로그

> 이 문서는 프로젝트의 설계·환경·규약을 요약한 인수인계 노트입니다.
> (VS Code + Claude Code로 이어서 작업할 때 이 파일을 먼저 읽히면 맥락을 빠르게 잡을 수 있습니다.)

## 1. 프로젝트 개요
- **목적**: 네이버 블로그/워드프레스의 제약 없이 SEO 태그·스크립트·구조화 데이터를 자유롭게 제어하기 위한 **자체 경량 블로그**.
- **핵심 가치**: 서버사이드 렌더링(JSP)로 SEO 친화, 인프라는 가볍게 유지.
- **운영 도메인**: https://tech.maillink.co.kr
- **저장소**: https://github.com/wintermaze2/AI_BLOG (main 브랜치)
  - ⚠️ 리포지토리 루트는 로컬 작업 폴더(`AI_BLOG/`)가 아니라 **`blogskeleton/blog/`** — 이 파일이 있는 위치입니다.

## 2. 기술 스택 (2026-08 기준)
| 구성 | 버전 | 비고 |
|---|---|---|
| OS | Amazon Linux 2023 (arm64) | AWS EC2 **t4g.small** (2 vCPU / 2GB) |
| JDK | Amazon Corretto 21 (LTS) | `maven.compiler.release=21` |
| 빌드 | Maven 3.9.x | WAR 패키징 |
| WAS | Tomcat 11.0 | Jakarta EE 11, Servlet 6.1 |
| DB | MySQL 8.4 LTS | utf8mb4, DB명 `blog` |
| 웹서버 | Nginx | 80/443 → 8080 리버스 프록시, HTTPS 종단 |
| 뷰 | JSP + JSTL 3.0 | **`jakarta.tags.core`** (구 javax 아님) |
| 라이브러리 | HikariCP, flexmark(MD→HTML), jBCrypt | |

## 3. 서버 환경 / 경로
- 소스: `~/blog` (빌드 작업 위치)
- Tomcat: `/opt/tomcat` (심볼릭 링크 → `/opt/apache-tomcat-11.0.24`)
- 배포 위치: `/opt/tomcat/webapps/ROOT.war` (도메인 루트 서비스)
- 배포 스크립트: `~/deploy.sh` (빌드→정지→교체→기동, 빌드 실패 시 중단)
- 메모리 튜닝: MySQL `innodb_buffer_pool_size=512M`, Tomcat `-Xms256m -Xmx640m`, 스왑 2GB

## 4. 아키텍처
얇은 **프론트 컨트롤러(MVC)** 구조. 프레임워크 없음(순수 Servlet + JSP).

```
요청 → DispatcherServlet("/")  ─ 예쁜 URL 라우팅 ─→ DAO → JSP 렌더링
        ├ "/"               홈(글 목록, ?page=N)
        └ "/posts/{slug}"   글 상세
      AdminServlet("/admin/*") ─ 로그인/CRUD (AuthFilter로 보호)
      SitemapServlet("/sitemap.xml"), RssServlet("/rss.xml")
      정적: /static/* , /robots.txt → 컨테이너 default 서블릿 (web.xml)
```

레이어: **Controller(Servlet) → DAO → JSP(View)**, 도메인 객체는 `model/`.

## 5. 디렉터리 구조
```
blog/
├── pom.xml                     # 의존성 + 빌드 (finalName=blog)
├── sql/schema.sql              # 스키마 + 샘플 데이터
├── deploy 관련은 서버 ~/deploy.sh
└── src/main/
    ├── java/com/example/blog/
    │   ├── controller/  DispatcherServlet, AdminServlet, SitemapServlet, RssServlet
    │   ├── filter/      AuthFilter (/admin/* 세션 보호)
    │   ├── dao/         PostDao, CategoryDao, AdminUserDao
    │   ├── model/       Post, Category, AdminUser
    │   ├── tool/        GenerateHash (BCrypt 해시 생성 CLI)
    │   └── util/        Database(HikariCP), MarkdownUtil, SlugUtil,
    │                    SiteConfig, PasswordUtil, AppContextListener
    └── webapp/
        ├── WEB-INF/web.xml
        ├── WEB-INF/views/  layout/(header,footer), post/(list,detail),
        │                   admin/(login,list,form), 404, error
        ├── static/css/style.css
        └── robots.txt
```

## 6. 데이터 모델 (요약)
- `post(id, slug, title, summary, content_md, content_html, thumbnail_url,
   meta_description, status[DRAFT|PUBLISHED], view_count, category_id,
   created_at, updated_at, published_at)`
- `category(id, name, slug)`, `tag`, `post_tag`(다대다)
- `admin_user(id, username, password_hash)` — BCrypt(cost 12)
- 본문은 `content_md`(원문)와 `content_html`(렌더링본)을 함께 저장 → 조회 시 파싱 비용 0.

## 7. SEO 처리 (중요)
- 예쁜 URL: `/posts/{slug}` (slug 컬럼)
- 글마다 `<title>`, `meta description`, `canonical`, Open Graph/Twitter (header.jsp)
- **JSON-LD(BlogPosting)** 는 `DispatcherServlet.buildJsonLd()` 에서 안전하게 생성해 detail.jsp에 주입
- 동적 `sitemap.xml`, `rss.xml`
- `X-Forwarded-Proto` 인식을 위해 Tomcat `server.xml`에 **RemoteIpValve** 설정(프록시 뒤 https 스킴 정확화)
- 관리자 페이지는 `noindex` + `robots.txt` Disallow

## 8. 관리자 기능
- `/admin/login` 로그인(세션) → `/admin` 목록 → `/admin/new`, `/admin/edit?id=` , save/delete
- 본문 입력: EasyMDE(Markdown, CDN), 저장 시 flexmark로 HTML 변환
- 보안: BCrypt, 세션 CSRF 토큰(save/delete), 로그인 시 `changeSessionId()`
- **최초 계정 생성**:
  ```bash
  mvn -q compile exec:java -Dexec.mainClass=com.example.blog.tool.GenerateHash -Dexec.args="비밀번호"
  # 출력 해시로:
  # INSERT INTO admin_user(username, password_hash) VALUES('admin','<해시>');
  ```

## 9. 빌드 & 배포 워크플로
**서버에서 빌드** (서버 소스 `~/blog` 기준):
```bash
cd ~/blog
mvn clean package            # → target/blog.war
~/deploy.sh                  # 정지 → ROOT 교체 → 기동 (내부에서 sudo 사용)
```
**로컬에서 빌드 후 원격 배포** (리포지토리에 포함된 `deployremote.sh`, Git Bash에서 실행):
```bash
bash deployremote.sh         # 로컬 mvn package → scp → 서버 Tomcat 재배포
```
빌드 산출물이 없으면 전송 전에 중단하므로 서버는 그대로 유지됩니다.
런타임 접속정보는 **환경변수**로 주입 (`/etc/systemd/system/tomcat.service` [Service]):
`BLOG_DB_URL, BLOG_DB_USER, BLOG_DB_PASS, BLOG_BASE_URL, BLOG_SITE_NAME`
→ 값 변경 시 `sudo systemctl daemon-reload && sudo systemctl restart tomcat`.
미설정 시 `Database.java`/`SiteConfig.java`의 기본값 사용.

## 10. 코딩 규약 / 주의점
- **Jakarta 네임스페이스** 필수: `jakarta.servlet.*`, JSTL taglib `jakarta.tags.core` / `jakarta.tags.functions` (구 `javax`/`java.sun.com` URI 사용 금지).
- 모든 SQL은 **PreparedStatement** (SQL 인젝션 방지).
- 사용자 입력 출력은 JSP에서 `<c:out>` / `fn:escapeXml` (XSS 방지). 단 `content_html`은 관리자 작성 신뢰 HTML이라 그대로 출력.
- slug는 `SlugUtil.toSlug()` (한글 허용, 공백/특수문자 → 하이픈). 중복 시 타임스탬프 suffix.
- DB 접속 실패 시 현재는 리스너에서 fail-fast (앱 미기동). 필요 시 견고화 여지 있음.

## 11. TODO / 다음 확장 후보
- [ ] 카테고리/태그별 목록 라우팅 (`/category/{slug}`, `/tag/{slug}`)
- [ ] 검색 기능
- [ ] 댓글, 인기글(조회수) 위젯
- [ ] DB 다운 시에도 앱 기동되도록 리스너 견고화(HikariCP `initializationFailTimeout`)
- [ ] 구글 서치 콘솔 등록 + sitemap 제출
- [x] Git 버전관리 도입 (§1 저장소 참고)
