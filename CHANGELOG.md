# 변경 이력

> 날짜순 작업 기록입니다. **현재 구조·규약**은 [PROJECT_NOTES.md](PROJECT_NOTES.md)를 보세요.
> 이 문서는 "언제 무엇을 왜 바꿨는지", 그리고 **어디까지 검증했는지**를 남깁니다.

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
