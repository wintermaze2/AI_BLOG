# PROJECT NOTES — 경량 JSP 블로그

> 이 문서는 프로젝트의 설계·환경·규약을 요약한 인수인계 노트입니다. **현재 상태**를 담습니다.
> (VS Code + Claude Code로 이어서 작업할 때 이 파일을 먼저 읽히면 맥락을 빠르게 잡을 수 있습니다.)
>
> 날짜순 작업 기록과 "왜 그렇게 바꿨는지", 검증 범위는 [CHANGELOG.md](CHANGELOG.md)에 있습니다.

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
        ├ "/"                홈(글 목록, ?page=N)
        ├ "/posts/{slug}"    글 상세
        ├ "/category/{slug}" 카테고리 아카이브(?page=N)
        ├ "/tag/{slug}"      태그 아카이브(?page=N)
        └ "/search?q=..."    검색 결과(?page=N)
      AdminServlet("/admin/*") ─ 로그인/CRUD
        └ 필터 체인(web.xml 선언, 순서 중요): AdminIpFilter → AuthFilter
      SitemapServlet("/sitemap.xml"), RssServlet("/rss.xml")
      정적: /static/* , /robots.txt → 컨테이너 default 서블릿 (web.xml)
```

레이어: **Controller(Servlet) → DAO → JSP(View)**, 도메인 객체는 `model/`.

## 5. 디렉터리 구조
```
blog/
├── pom.xml                     # 의존성 + 빌드 (finalName=blog)
├── SEO.md                      # 검색엔진 등록 절차
├── CHANGELOG.md                # 변경 이력
├── sql/schema.sql              # 스키마 + 카테고리 초기 데이터
├── deploy 관련은 서버 ~/deploy.sh
└── src/main/
    ├── java/com/example/blog/
    │   ├── controller/  DispatcherServlet, AdminServlet, SitemapServlet, RssServlet
    │   ├── filter/      AdminIpFilter(IP 허용목록), AuthFilter(세션 보호),
    │   │                SearchRateLimitFilter(/search 속도 제한)
    │   ├── dao/         PostDao, CategoryDao, TagDao, AdminUserDao
    │   ├── model/       Post, Category, Tag, AdminUser
    │   ├── tool/        GenerateHash (BCrypt 해시 생성 CLI)
    │   └── util/        Database(HikariCP), MarkdownUtil, SlugUtil, UrlUtil, JsonLd,
    │                    RateLimiter, SiteConfig, PasswordUtil, AppContextListener
    └── webapp/
        ├── WEB-INF/web.xml
        ├── WEB-INF/views/  layout/(header,footer,fonts), post/(list,detail),
        │                   admin/(login,list,form), 404, error
        ├── static/css/style.css
        └── robots.txt
```

## 6. 데이터 모델 (요약)
- `post(id, slug, title, summary, content_md, content_html, thumbnail_url,
   meta_description, status[DRAFT|PUBLISHED], view_count, category_id,
   created_at, updated_at, published_at)`
- `category(id, name, slug)`, `tag(id, name, slug)`, `post_tag`(다대다)
  - 태그는 글 저장 시 `TagDao.syncPostTags()`가 트랜잭션으로 재동기화(기존 연결 삭제 → 태그 upsert → 재연결).
    태그 식별 기준은 **slug** 이므로 "Java"와 "java"는 같은 태그로 합쳐진다.
  - 어느 글에도 붙지 않게 된 태그는 `TagDao.deleteOrphans()`가 정리한다
    (태그 동기화 트랜잭션 안에서, 그리고 글 삭제 직후에 호출).
- `admin_user(id, username, password_hash)` — BCrypt(cost 12)
- 본문은 `content_md`(원문)와 `content_html`(렌더링본)을 함께 저장 → 조회 시 파싱 비용 0.
- `content_type`(MD | HTML)이 원문을 어떻게 다룰지 정한다.
  - `MD`: 저장 시 flexmark로 변환해 `content_html` 생성
  - `HTML`: 변환 없이 입력을 그대로 `content_html`에 저장
  - 어느 쪽이든 원문은 `content_md`에 보관해 재편집 시 그대로 다시 보여준다
  - **배포 순서 주의**: 모든 글 조회 쿼리가 `content_type`을 SELECT하므로,
    WAR 배포 **전에** `sql/schema.sql`의 마이그레이션(ALTER TABLE)을 먼저 적용해야 한다.
    순서가 뒤바뀌면 컬럼이 없어 모든 페이지가 500을 낸다.

## 7. SEO 처리 (중요)

> 검색엔진 등록 절차와 등록 전 점검 사항은 [SEO.md](SEO.md) 에 별도로 정리했습니다.
- 예쁜 URL: `/posts/{slug}` (slug 컬럼)
- 글마다 `<title>`, `meta description`, `canonical`, Open Graph/Twitter (header.jsp)
- **JSON-LD**는 `util/JsonLd` 에서 생성한다(이스케이프를 한 곳에 모아 `</script>` 주입을 막음).
  - 글 상세: `BlogPosting` + `BreadcrumbList`
  - 홈 1페이지: `WebSite` + `SearchAction`(사이트 내 검색)
- 동적 `sitemap.xml`(글 + **발행 글이 있는** 카테고리/태그 아카이브), `rss.xml`
- slug에 한글이 허용되므로 문서에 박히는 절대 URL(canonical/OG/sitemap/RSS)은 `UrlUtil.encodePath()`로
  퍼센트 인코딩한다. 반대로 들어오는 요청은 `getRequestURI()`가 인코딩된 원문이라
  `DispatcherServlet.decodeSlug()`로 디코딩해야 DB의 slug와 일치한다.
- `X-Forwarded-Proto` 인식을 위해 Tomcat `server.xml`에 **RemoteIpValve** 설정(프록시 뒤 https 스킴 정확화)
- 관리자 페이지는 `noindex` + `robots.txt` Disallow
- **얇은 페이지는 `noindex, follow`**: 검색 결과(`/search`)는 항상, 카테고리/태그 아카이브는
  결과가 0건일 때. 서블릿이 `noindex` 속성을 세팅하면 header.jsp가 meta 태그를 출력한다.
  검색 결과에는 canonical도 넣지 않는다.

## 8. 관리자 기능
- `/admin/login` 로그인(세션) → `/admin` 목록 → `/admin/new`, `/admin/edit?id=` , save/delete
- 본문 입력: 형식 선택(Markdown / HTML) + `.md`/`.html` 파일 불러오기(FileReader, UTF-8)
  - Markdown일 때만 EasyMDE(CDN)를 붙인다. HTML 형식이면 에디터를 떼고 일반 textarea로 둔다
    (EasyMDE는 CodeMirror 기반이라 HTML 소스 편집에 방해가 되고, 값 주입 경로도 다르다)
  - 파일을 불러오면 확장자에 맞춰 형식 드롭다운을 자동으로 맞춘다
    (`.html`을 Markdown으로 둔 채 저장하면 flexmark가 원문을 뭉갠다)
- 태그 입력: 쉼표(또는 줄바꿈) 구분 텍스트, 글당 최대 20개. 저장 시 slug로 정규화해 동기화
- 보안: BCrypt, 세션 CSRF 토큰(save/delete), 로그인 시 `changeSessionId()`
- **IP 허용목록**: `/admin/*` 는 허용 IP에서만 접근 가능(그 외 404). 인증보다 먼저 검사한다.
  - 기본 허용: `211.239.43.40`, `58.121.175.130` (`AdminIpFilter.DEFAULT_ALLOWED_IPS`)
  - 환경변수 `BLOG_ADMIN_ALLOWED_IPS`(쉼표 구분)로 재빌드 없이 덮어쓸 수 있다
  - 판정 기준은 `getRemoteAddr()`이며, **Nginx 뒤이므로 Tomcat RemoteIpValve가 필수**다.
    밸브가 없으면 모든 요청이 127.0.0.1로 보여 아무도 못 들어간다
  - **잠겼을 때**: `sudo grep AdminIpFilter /opt/tomcat/logs/catalina.out` 로 관측된 IP를 확인 →
    systemd에 `BLOG_ADMIN_ALLOWED_IPS` 설정 후 `daemon-reload && restart tomcat`
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
`BLOG_DB_URL, BLOG_DB_USER, BLOG_DB_PASS, BLOG_BASE_URL, BLOG_SITE_NAME, BLOG_SITE_DESC,
 BLOG_ADMIN_ALLOWED_IPS, BLOG_SEARCH_RATE_PER_MIN, BLOG_SEARCH_RATE_BURST,
 BLOG_GOOGLE_SITE_VERIFICATION, BLOG_NAVER_SITE_VERIFICATION,
 BLOG_DEFAULT_OG_IMAGE, BLOG_LOGO_URL`
→ 값 변경 시 `sudo systemctl daemon-reload && sudo systemctl restart tomcat`.
미설정 시 `Database.java`/`SiteConfig.java`의 기본값 사용.

## 10. 코딩 규약 / 주의점
- **Jakarta 네임스페이스** 필수: `jakarta.servlet.*`, JSTL taglib `jakarta.tags.core` / `jakarta.tags.functions` (구 `javax`/`java.sun.com` URI 사용 금지).
- 모든 SQL은 **PreparedStatement** (SQL 인젝션 방지).
- **검색 입력 방어** (`/search`): 세 겹으로 막는다.
  1. 값 바인딩은 PreparedStatement — SQL 인젝션 차단
  2. `PostDao.likePattern()` 이 `%`·`_`·`\` 를 이스케이프 — 와일드카드 남용 차단
     (안 하면 `%` 한 글자로 전체 글을 긁어갈 수 있다)
  3. 화면 출력은 `<c:out>` / `fn:escapeXml` — XSS 차단
  검색어는 100자로 자르고, 결과 페이지는 `noindex`.
  키워드 블랙리스트는 두지 않는다 — 위 세 겹으로 구조적으로 막히고,
  블랙리스트는 우회는 쉬운 반면 정상 검색어를 막는 부작용이 크다.
- **검색 속도 제한** (`SearchRateLimitFilter`): 검색은 `LIKE '%..%'` 로 본문 전체를 훑어
  인덱스를 못 타므로, 짧은 시간에 쏟아지면 그 자체가 서버 부하 공격이 된다.
  IP별 토큰 버킷으로 제한하고 초과 시 429 + `Retry-After`.
  - 기본값: 분당 20회, 버스트 10회 → `BLOG_SEARCH_RATE_PER_MIN`, `BLOG_SEARCH_RATE_BURST`
  - 추적 IP는 2만 개 상한. 넘으면 유휴 항목부터 정리하고, 그래도 넘치면
    새 IP는 추적하지 않고 통과시킨다(메모리 우선). 추적 중인 IP의 제한은 유지된다.
  - `RateLimiter` 는 범용 유틸이라 로그인 시도 제한 등에도 재사용할 수 있다.
- 사용자 입력 출력은 JSP에서 `<c:out>` / `fn:escapeXml` (XSS 방지). 단 `content_html`은 관리자 작성 신뢰 HTML이라 그대로 출력.
- **폰트**: 본문 Noto Sans KR, 코드 JetBrains Mono (Google Fonts).
  - 로딩은 `layout/fonts.jsp` 한 곳에서만 하고, `<head>`를 직접 가진 화면
    (`layout/header.jsp`, `admin/login·list·form.jsp`)이 정적 include 로 끌어 쓴다.
  - CSS에서는 `var(--sans)` / `var(--mono)` 로만 참조한다(style.css `:root`에 정의).
  - Noto Sans KR은 `wght@400..800` 가변 축으로 받는다. 사이트가 600·800도 쓰는데
    그 두께가 없으면 브라우저가 굵기를 합성해 한글이 뭉개진다.
- slug는 `SlugUtil.toSlug()` (한글 허용, 공백/특수문자 → 하이픈). 중복 시 타임스탬프 suffix.
- DB 접속 실패 시 현재는 리스너에서 fail-fast (앱 미기동). 필요 시 견고화 여지 있음.

## 11. TODO / 다음 확장 후보
- [x] 카테고리/태그별 목록 라우팅 (`/category/{slug}`, `/tag/{slug}`)
- [x] 검색 기능 (`/search?q=`) — 제목/요약/본문 LIKE 검색.
      글이 많아지면 `LIKE '%..%'`가 인덱스를 못 타므로 **FULLTEXT(ngram) 전환** 검토
      (한글은 기본 파서로 토큰화가 안 되어 `WITH PARSER ngram` 필요)
- [ ] 댓글, 인기글(조회수) 위젯
- [ ] DB 다운 시에도 앱 기동되도록 리스너 견고화(HikariCP `initializationFailTimeout`)
- [ ] 구글 서치 콘솔 등록 + sitemap 제출
- [x] Git 버전관리 도입 (§1 저장소 참고)
