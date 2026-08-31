# 변경 이력

> 날짜순 작업 기록입니다. **현재 구조·규약**은 [PROJECT_NOTES.md](PROJECT_NOTES.md)를 보세요.
> 이 문서는 "언제 무엇을 왜 바꿨는지", 그리고 **어디까지 검증했는지**를 남깁니다.
>
> 항목 뒤 표시는 운영 반영 상태입니다.
> `pending` 코드/문서만 바뀐 상태 · `applied` 운영 서버(DB 또는 WAR)까지 반영됨

---

## 2026-08-28

### 기능

**글 상세에 SNS 공유 버튼 추가** `applied 2026-08-28`
- 페이스북·쓰레드는 intent URL 이라 링크만으로 동작한다(JS 불필요)
- **인스타그램은 웹 공유 URL 이 아예 없다.** 외부 링크를 받는 엔드포인트를
  제공하지 않으므로, 모바일 OS 공유 시트(navigator.share)로만 보낼 수 있다.
  그래서 네이티브 공유 버튼을 넣었다 — 지원 브라우저에서만 드러나며
  인스타그램·카카오톡을 포함해 설치된 앱 전부를 띄운다
- 카카오톡은 Kakao SDK 가 필요해 `BLOG_KAKAO_JS_KEY` 가 있을 때만 버튼과
  SDK 를 출력한다(소유확인 토큰과 같은 방식). 미설정이 기본이므로 제3자
  스크립트가 실리지 않는다. `sendScrap` 을 써서 페이지 og 태그를 그대로 쓴다
  - SDK 는 2.8.2 로 고정하고 integrity(SRI)를 걸었다. 해시는 CDN 파일을
    직접 내려받아 계산한 값이다. 버전을 올리면 해시도 다시 계산해야 한다
- 링크 복사 버튼 추가. clipboard API 를 못 쓰는 환경 폴백 포함
- 공유 URL 은 서버에서 URLEncoder 로 인코딩해 넘긴다.
  EL 에 URL 인코딩 함수가 없어 한글 제목이 깨진다
- static/js 신설(이 프로젝트의 첫 JS 파일). `/static/*` 매핑이 이미 있어
  web.xml 변경은 없다
- 배포 후 공유 영역이 "스타일이 안 먹은 것"처럼 보였다. CSS 는 정상 배포되어
  있었고 원인은 디자인이었다. 바로 위 `.post-tags a` 와 공유 버튼이 둘 다
  회색 알약(border-radius:999px + var(--line) 테두리)이라 공유 버튼이 태그의
  두 번째 줄처럼 읽혔다. 카드로 묶고 플랫폼 브랜드색을 입혀 분리했다
- **카카오 도메인 등록은 두 곳에 각각 해야 한다.** [앱] > 제품 링크 관리 와
  [앱] > JavaScript SDK 도메인 은 서로 독립이다. 제품 링크 관리에만 등록하고
  SDK 도메인을 빠뜨려 `Error Code 4019` 가 났다. 오류 안내가 제품 링크 관리만
  언급해서 원인을 찾기 어려웠다. 자세한 내용은 SEO.md
- 검증: mvn -o package (JspC 포함) 통과. 배포 후 실제 조회로 확인 —
  og:image 출력, twitter:card 가 summary_large_image 로 승격, JSON-LD
  publisher.logo 반영, 카카오 SDK 2.8.2 로드, 선언한 SRI 해시가 CDN 파일과 일치,
  공유 버튼 5종 출력. 카카오톡 공유 창까지 정상 동작 확인
- 환경변수 3건(BLOG_KAKAO_JS_KEY / BLOG_DEFAULT_OG_IMAGE / BLOG_LOGO_URL)을
  넣고 재배포했는데 반영되지 않았다. deployremote.sh 가 stop/start 만 하고
  `daemon-reload` 를 하지 않아 systemd 가 예전 유닛 파일을 계속 썼기 때문이다.
  **유닛 파일을 고친 뒤에는 daemon-reload 후 restart 해야 한다**

### SEO

**기본 og:image 와 publisher 로고 추가** `applied 2026-08-28`
- `static/img/og-default.png` (1200x630) 추가. 설정하면 대표 이미지가 없는
  글도 공유 카드에 이미지가 나가고 twitter:card 가 summary_large_image 로 올라간다
- `contents/maillink_symbol_512.png` 를 `static/img/logo-512.png` 로 옮김.
  BLOG_LOGO_URL 권장값을 favicon-192 에서 이쪽으로 바꿨다
- SEO.md 갱신: §0(1) 슬러그 오타는 해결됐으므로 표시 변경, §0(2) 카테고리
  표에 문자보안 추가, §1 환경변수 표에 BLOG_KAKAO_JS_KEY 추가,
  §4 공유 미리보기에 페이스북·카카오 디버거와 캐시 갱신 안내 추가
- 환경변수 두 건 설정 완료. 배포 후 실제 조회로 확인했다

### 콘텐츠

**문자보안 카테고리와 스미싱 글 추가** `pending`
- 문자 발송 비중이 이메일과 비슷해져 `문자보안`(sms-security) 카테고리를 신설
- 첫 글 what-is-smishing: 정의 -> 6단계 공격 흐름과 사례 4종 -> 개인/기업
  대응 -> 메일링크의 예방 기능 5가지. 각 기능이 6단계 중 어디를 끊는지 대응시켰다
- guide.txt 19번(맺음 CTA) 개정: 링크·버튼 문구·구조는 고정하되
  `p.cta-copy` 한 문장만 글 주제에 맞게 바꿀 수 있게 했다.
  스미싱 글에서 "안전한 문자 발송도 메일링크에서" 를 쓰기 위한 변경이다
- 카테고리 INSERT 는 **아직 운영 DB 에 적용 전**

---

## 2026-08-21

### SEO

**검색결과 파비콘 수정** `applied 2026-08-21`
- 증상: 구글 검색결과에 메일링크 아이콘 대신 브라우저 기본 아이콘이 나왔다
- 원인: 파비콘으로 헤더 로고 `logo-bg-trans.png` 를 그대로 선언하고 있었는데
  이 이미지가 **50x37 로 정사각형이 아니다**. 구글은 정사각형 아이콘만 받고
  48의 배수를 권장하므로, 조건에 걸린 아이콘은 기본값으로 대체된다
- WAR 루트에 아이콘 3종을 두고 홈 `<head>` 선언을 교체
  - `/favicon.ico` (16~256 6종), `/favicon-192.png` (192x192),
    `/apple-touch-icon.png` (180x180)
  - 브라우저와 iOS 가 관례적으로 찾는 경로와 같게 두어, 선언을 못 읽어도 잡힌다
- 헤더에 보이는 브랜드 로고는 그대로 둔다. 파비콘과 용도가 다르다
- **web.xml 매핑 필수**: DispatcherServlet 이 `/` 를 잡으므로 루트 정적 파일은
  경로마다 default 서블릿 매핑을 넣어야 한다. 처음엔 파일만 두고 매핑을 빼먹어
  `/favicon-192.png` 와 `/apple-touch-icon.png` 가 404 였다(1차 배포 후 발견).
  `/favicon.ico` 는 매핑이 이미 있어 정상이었다.
  앞으로 루트에 정적 파일을 추가할 때마다 web.xml 에 함께 등록해야 한다
- 부수 효과: 192x192 가 구글 publisher 로고 최소 크기(112px)를 넘으므로
  이제 `BLOG_LOGO_URL` 을 설정할 수 있다. SEO.md 에 반영.
  `BLOG_DEFAULT_OG_IMAGE` 는 1200x630 가로 이미지가 따로 필요해 여전히 보류
- 검증: `mvn -o package` (JspC 포함) 통과, WAR 루트에 3개 파일 포함 확인.
  **재배포 후** 구글이 홈을 다시 크롤링해야 검색결과에 반영된다(며칠~몇 주)

### 스타일

**본문 h4 스타일 추가** `applied 2026-08-21`
- `.post-body` 헤딩 규칙이 h1~h3 만 잡고 있어 h4 가 브라우저 기본값으로 렌더링됐다.
  기본 h4 는 본문(1rem)보다 작아져 h3 와 본문 사이 단계가 성립하지 않는다
- h4 를 규칙에 넣고 `font-size:1.02rem` 로 올림.
  ARC / 이메일 헤더 분석 글이 h4 를 쓰므로 **WAR 재배포 후에** 발행해야 한다

### 콘텐츠 / DB

**브랜드 태그 규칙 도입** `applied 2026-08-21`
- 모든 글의 태그에 `메일링크`, `넷퍼씨` 를 넣기로 하고 guide.txt 에 규칙을 명시.
  [A] 20번 태그 항목과 발행 절차 5단계(실제 누락이 나는 지점) 두 곳에 적었다
- 태그는 `/tag/{슬러그}` 아카이브와 글 상세 JSON-LD 의 keywords 로 나가므로
  브랜드 태그가 전체 글을 묶는 축이 된다
- guide.txt 발행 절차 5단계가 제출 항목을 "[A] 18번"으로 잘못 참조하던 것을
  20번으로 수정(18번은 맺음 CTA 블록)
- 검증: 기존 발행 글 태그는 관리자에서 수동 반영 완료(2026-08-21).
  SlugUtil 이 한글을 그대로 두므로 태그 슬러그는 `메일링크`/`넷퍼씨` 가 되고,
  sitemap 에는 UrlUtil.encodePath 를 거쳐 퍼센트 인코딩된 URL 로 나간다

**카테고리 '이메일 보안' 추가, 샘플 카테고리 제거** `applied 2026-08-21`
- 운영 카테고리를 `이메일 인증`(email-auth), `이메일 작성팁`(sending-tips),
  `이메일 보안`(email-security) 셋으로 정리
- 샘플 데이터(카테고리 `개발`/`일상`, 글 hello-world/second-post)를 schema.sql 에서 제거
  - **이유**: 샘플 글 INSERT 가 `category_id = 1` 을 하드코딩한다. 운영 DB 에서
    `개발`(id 1) 을 지운 뒤 schema.sql 을 재실행하면 카테고리는 AUTO_INCREMENT 로
    새 id 를 받으므로 id 1 이 없어 FK 제약 위반으로 실패한다
- SEO.md §0 (2) 갱신 — "카테고리 미지정" 전제가 현재와 맞지 않아 카테고리 표로 교체.
  slug 변경은 색인 후 404 를 만든다는 경고 추가(301 리다이렉트 기능 없음)
- 검증: **운영 DB 반영 완료(2026-08-21)**. 실행한 SQL 두 건은
  카테고리 추가(INSERT ... ON DUPLICATE KEY UPDATE)와
  삭제(DELETE FROM category WHERE slug IN ('dev','life')).
  post.category_id 는 `ON DELETE SET NULL` 이라 글은 남고 카테고리만 해제된다

---

## 2026-08-15

### SEO

**검색엔진 등록 준비 및 구조화 데이터 보강** `pending`
- 배포된 사이트를 실제로 조회해 점검. 등록 전 고쳐야 할 것 두 가지 발견
  - 슬러그 오타 `hot-to-set-dns` (how 의 오타). 색인 후에는 URL 변경 비용이 크므로 지금 수정
  - 발행 글 5편 모두 카테고리 미지정 → 카테고리 아카이브가 비어 sitemap 에도 없음
- 소유확인 meta 지원: BLOG_GOOGLE_SITE_VERIFICATION / BLOG_NAVER_SITE_VERIFICATION.
  토큰이 없으면 태그를 아예 출력하지 않는다
- `util/JsonLd` 신설. DispatcherServlet 에 흩어져 있던 문자열 조립을 옮기고
  이스케이프를 한 곳으로 모음. `</script>` 는 `<` 로 치환해 스크립트 조기 종료 차단
  - 글 상세에 BreadcrumbList 추가, 홈 1페이지에 WebSite + SearchAction 추가
  - BlogPosting 에 articleSection(카테고리), keywords(태그) 추가
- og:image 가 없으면 twitter:card 를 summary 로 낮춤.
  large 로 선언해 놓고 이미지가 없으면 공유 시 빈 영역만 남기 때문
- publisher.logo 와 기본 og:image 는 **설정했을 때만** 출력.
  파비콘용 로고(50x37)는 두 용도 모두에 작아 구글 경고를 유발한다
- sitemap 홈 항목에 lastmod 추가(최근 글 수정 시각)
- SiteConfig 기본 사이트 설명을 개발용 문구에서 실제 주제에 맞게 교체
- SEO.md 신설 — 등록 전 점검, 환경변수, 구글/네이버 등록 절차, 등록 후 확인
- 검증: JSON-LD 4종이 유효한 JSON 인지 자체 파서로 확인, esc() 6개 케이스
  (`</script>` 무력화, 따옴표·역슬래시·개행·제어문자·null). mvn/JspC 통과

---

## 2026-08-14

### 보안

**관리자 페이지 IP 허용목록 차단** `f95ad27`
- `/admin/*` 을 지정 IP에서만 접근 가능하게 함. 허용: `211.239.43.40`, `58.121.175.130`
- 차단 응답은 403이 아니라 **404** — 403은 "여기 관리자 페이지가 있다"를 알려주는 셈이라 존재 자체를 숨김
- 필터 순서를 위해 `AdminIpFilter`/`AuthFilter` 를 **web.xml로 이동**.
  애노테이션 필터 간 실행 순서는 스펙상 보장되지 않는데 IP 검사가 인증보다 먼저 돌아야 함
- `X-Forwarded-For` 를 앱에서 직접 읽지 않고 `getRemoteAddr()` 사용.
  프록시 검증 없이 헤더를 믿으면 누구나 위조해 우회 가능하기 때문 (RemoteIpValve 의존)
- 잠김 대비: 차단 시 관측된 IP를 로그로 남기고, `BLOG_ADMIN_ALLOWED_IPS` 로 재빌드 없이 복구 가능
- 검증: IP 정규화·파싱 14개 케이스. **컨테이너 상의 필터 순서와 실제 `getRemoteAddr()` 값은 미검증**

**검색 요청 속도 제한** `4cd215c`
- 검색은 유일하게 입력값으로 본문 전체를 훑는 경로. `LIKE '%..%'` 는 인덱스를 못 타므로
  짧은 시간에 쏟아지면 그 자체가 부하 공격 (t4g.small, 2GB)
- `RateLimiter`(범용 토큰 버킷) + `SearchRateLimitFilter`. 기본 분당 20회 / 버스트 10회
- 고정 구간 카운터 대신 토큰 버킷 — 구간 경계에서 두 배가 통과하는 문제가 없음
- 제한기 자체가 메모리 고갈 경로가 되지 않도록 추적 IP 2만 개 상한.
  초과 시 유휴 항목 정리 → 그래도 넘치면 새 키는 미추적 통과(기존 키 제한은 유지)
- 차단 로그는 첫 건과 100건마다만 (공격 중 로그 폭주 방지)
- 검증: 18개 케이스 통과 — 버스트 상한, 회복 속도, 키 독립성, 시계 역행 방어,
  메모리 상한 fail-open, 유휴 정리, 동시 100요청 시 정확히 capacity 만큼만 통과.
  **컨테이너 상의 429 응답은 미검증**

### 기능

**본문 형식 선택 (Markdown / HTML)** `5993a7e`
- `post.content_type VARCHAR(10) NOT NULL DEFAULT 'MD'` 추가.
  `schema.sql` 에 CREATE 반영 + 운영 DB용 멱등 마이그레이션(information_schema 확인 후 ALTER)
- HTML 선택 시 flexmark 변환을 건너뛰고 입력을 그대로 `content_html` 에 저장.
  원문은 형식과 무관하게 `content_md` 에 보관해 재편집 시 복원
- 관리자 폼에 형식 드롭다운과 `.md`/`.html` 파일 불러오기(FileReader, UTF-8)
- EasyMDE는 Markdown일 때만 붙임. HTML이면 `toTextArea()` 로 떼어냄 —
  CodeMirror가 값을 따로 관리해 textarea 직접 대입이 화면에 반영되지 않기 때문
- 확장자로 형식 자동 전환 (`.html` 을 Markdown으로 저장하면 flexmark가 원문을 뭉갬)
- ⚠️ **배포 순서**: 모든 조회 쿼리가 `content_type` 을 SELECT하므로 마이그레이션이 WAR보다 먼저
- 검증: 빌드 + JspC. **마이그레이션 실행과 브라우저 동작은 미검증**

### 디자인

**파비콘·로고·브랜딩** `9e1172b`
- 로고를 `static/img/` 로 복사(원본 `contents/` 는 WAR에 패키징되지 않음), 전 화면 favicon 적용
- 타이틀 앞 로고 배치, 상단 RSS 링크 제거, '메일링크 바로가기' 버튼 추가
- head의 RSS `<link rel="alternate">` 는 유지 — 피드 리더 자동 검색용
- 푸터를 `메일링크 기술블로그 © Netpathy,Inc.` 로 변경

**웹폰트 전면 적용** `ae1a29f`
- Noto Sans KR + JetBrains Mono. 로딩은 `layout/fonts.jsp` 한 곳, `<head>` 를 가진 4개 화면이 정적 include
- `wght@400..800` 가변 축으로 요청 — 사이트가 600·800도 쓰는데 없으면 브라우저가 굵기를 합성해 한글이 뭉개짐
- CSS는 `var(--sans)` / `var(--mono)` 로만 참조

**헤더 로고 정렬·버튼 스타일** `b4fd35c`
- 로고 높이를 `26px` 고정 → `.95em` (글자 크기에 묶임) + `translateY(.07em)`.
  한글은 라인박스 중앙보다 아래에 앉아 로고만 떠 보였음
- 타이틀 `font-weight` 800 → 700 — 이 크기의 한글에 800은 'ㄹ' 가로획이 붙어 뭉개짐
- 바로가기 버튼을 검색창과 같은 외곽선·라운드로. `line-height` 고정(a는 body의 1.75를 물려받아 혼자 커짐)
- 검증: 헤드리스 Chrome 렌더링 후 픽셀 측정 — 로고/글자 잉크 경계 하단 0px, 상단 0.5px

**본문 표 공통 스타일** `44f3fe8`
- `.post-body table` 기본 스타일이 없어 글마다 표 CSS를 다시 만들어야 했음
- `.table-wrap`(가로 스크롤) / `.table-wide`(최소 너비) / `.scroll-hint`(모바일 안내) 추가
- DMARC 글에서 중복 표 스타일 제거, 지침서에서 관련 규칙 삭제
- 검증: 390/768/1280px 렌더링 측정 — 페이지 가로 스크롤 0, 넓은 표만 컨테이너 내부 스크롤

**목록 제목 색상** `2c26f6f`
- 목록 제목이 `<a>` 라 전역 링크 강조색을 물려받고 있었음 → `#16181d`
- 검증: 계산된 색 확인 (제목 `#16181d`, 카테고리 `#2f6fed` 유지)

### 콘텐츠 · 문서

**맺음 CTA 공통화 + 문구 변경** `applied 2026-08-21`
- 모든 글 끝에 '메일링크 바로가기' CTA를 넣도록 지침에 규칙 추가(18·19번).
  필수가 된 이상 글마다 CSS를 복사하면 안 되므로 `.cta`/`.cta-copy`/`.cta-btn` 을
  style.css 로 이동(표 때와 같은 판단)
- 문구를 'SPF·DKIM·DMARC 설정을 지원하는 대량메시징 플랫폼' →
  '이메일, 문자, FAX, 카카오 비즈메시지를 한 번에' 로 변경.
  특정 주제에 묶이지 않는 브랜드 문구라 모든 글에 공통 적용 가능
- 기존 글 4편에서 중복 CTA CSS 제거(파일당 8~11개 규칙), 문구 교체
- 새로 추가된 image-only-email-spam.html 에는 CTA가 없어 규칙에 맞춰 삽입
- 지침서 5번(꼬리말 금지)에 CTA 예외 명시, 체크리스트·참고자료 갱신
- 검증: 전용 CSS를 걷어낸 상태로 렌더링해 CTA 정상 표시 확인,
  4개 조각 모두 지침서 검사 명령 통과

- `contents/` 버전관리 편입 `8a6c84c`
- DMARC 글 본문 조각 `96b87b2` — 완결 HTML 문서를 본문용으로 변환.
  `:root` 의 `--muted` 가 블로그 변수와 겹쳐 그대로 넣었으면 헤더·푸터 색까지 바뀔 상황이었음
- **본문 HTML 작성 지침서** `3d5eda8` — [contents/guide.txt](contents/guide.txt).
  LLM에 그대로 붙여넣는 프롬프트 + 점검 체크리스트 + 검사 명령 3종
- 글 초안 3편 (SPF, DKIM, DNS 설정) `6d181ae`

---

## 2026-08-13

**초기 구축** `05fc8ac`
- 경량 JSP 블로그 골격: 프론트 컨트롤러, DAO, JSP 뷰, 관리자 CRUD

**저장소 정비** `a6d9d75`
- `.gitignore` 추가 — `target/`(59MB)이 커밋될 위험 제거
- `.gitattributes` 추가 — `core.autocrlf=true` 환경에서 clone 시 셸 스크립트가 CRLF로
  체크아웃되어 실행이 깨지는 것 방지
- `deployremote.sh` 추적 시작, `README.md.bak` 추적 해제

**카테고리/태그 라우팅** `7b0e026`
- `/category/{slug}`, `/tag/{slug}` 추가. 공유 `renderList()` 로 페이징·SEO 일원화
- `tag`/`post_tag` 는 스키마만 있고 DAO·UI가 없어 태그를 붙일 방법이 없었으므로
  `TagDao.syncPostTags()`(트랜잭션)와 관리자 입력란까지 함께 구현
- **기존 버그 수정**: `getRequestURI()` 는 인코딩된 원문을 주는데 디코딩하지 않아
  한글 slug 글이 404였음. `decodeSlug()` 추가, 반대로 절대 URL은 `UrlUtil.encodePath()`

**검색 + 고아 태그 정리** `0f8f2d0`
- `/search?q=` — 제목/요약/본문 부분 일치, 제목 우선 정렬
- 입력의 `%`·`_` 를 이스케이프하고 `ESCAPE` 절 명시 (안 하면 `%` 한 글자로 전체 조회)
- **기존 버그 수정**: 페이지 링크가 `?page=N` 이라 쿼리를 통째로 덮어써,
  검색 2페이지로 가면 `q` 가 사라졌음 → `pageLinkBase` 로 유지
- 검색 결과는 `noindex,follow` + canonical 없음. 결과 0건 아카이브도 noindex(soft-404 방지)
- `TagDao.deleteOrphans()` — 어느 글에도 안 붙은 태그 정리

---

## 검증 수단

이 프로젝트에서 쓰는 확인 방법입니다.

| 대상 | 방법 |
|---|---|
| Java 컴파일·패키징 | `mvn clean package` |
| JSP 문법·taglib·EL | Jasper `JspC` 선컴파일 (`mvn package` 는 JSP를 건드리지 않음) |
| CSS·레이아웃 | 헤드리스 Chrome 렌더링 후 스크린샷 픽셀 / 계산된 스타일 측정 |
| 순수 로직 | 임시 main 클래스로 케이스 검증 (RateLimiter, IP 파싱, LIKE 이스케이프) |
| 본문 조각 규칙 | `contents/guide.txt` 의 검사 명령 3종 |

**DB가 필요한 것은 로컬에서 검증할 수 없습니다** (로컬에 MySQL·Docker 없음).
마이그레이션 실행, 검색 SQL, 다중 테이블 삭제 구문, 컨테이너 상의 필터 동작은
배포 후 확인이 필요합니다.
