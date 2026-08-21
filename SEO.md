# SEO 운영 가이드

> 검색엔진 등록 절차와 등록 전 점검 사항입니다.
> 코드에 이미 들어 있는 SEO 요소는 [PROJECT_NOTES.md](PROJECT_NOTES.md) §7 을 보세요.

---

## 0. 등록 전에 반드시 처리할 것

색인이 시작된 뒤에 URL을 바꾸면 쌓인 신호를 잃고 404가 남습니다.
이 블로그에는 301 리다이렉트 기능이 없으므로 **등록 전에** 정리하세요.

### (1) 슬러그 오타 — `hot-to-set-dns`

DNS 설정 글의 슬러그가 `hot-to-set-dns` 로 등록되어 있습니다. `how` 의 오타입니다.

```
현재 : https://tech.maillink.co.kr/posts/hot-to-set-dns   (200)
정상 : https://tech.maillink.co.kr/posts/how-to-set-dns   (현재 404)
```

관리자 > 해당 글 수정 > Slug 를 `how-to-set-dns` 로 바꾸고 저장하세요.
아직 이 URL을 링크하는 글이 없어 지금 고치면 부작용이 없습니다.

### (2) 카테고리

`2026-08-21` 기준으로 아래 세 개를 씁니다. 관리자 화면에 카테고리를 만드는
기능이 없으므로, 추가·변경은 [sql/schema.sql](sql/schema.sql) 의
"카테고리 초기 데이터" 블록을 고치고 DB에 직접 반영합니다.

| 이름 | slug | 아카이브 URL |
|---|---|---|
| 이메일 인증 | `email-auth` | `/category/email-auth` |
| 이메일 작성팁 | `sending-tips` | `/category/sending-tips` |
| 이메일 보안 | `email-security` | `/category/email-security` |

기본 카테고리 `개발`(dev) / `일상`(life) 은 이 블로그 주제와 맞지 않아 삭제했습니다.

**글마다 카테고리가 지정되어 있는지 확인하세요.** 비어 있으면

- 글 상세·목록에 카테고리 표시가 안 나옴
- `/category/{slug}` 아카이브가 비고 sitemap 에도 안 들어감
  (발행 글 0건인 카테고리는 의도적으로 제외됩니다)
- 내부 링크 구조가 약해짐 (검색엔진이 주제 묶음을 파악하기 어려움)

```sql
SELECT c.name, c.slug, SUM(p.status = 'PUBLISHED') AS 발행글
  FROM category c LEFT JOIN post p ON p.category_id = c.id
 GROUP BY c.id, c.name, c.slug ORDER BY c.id;
```

**카테고리는 색인 전에 확정하세요.** 색인이 시작된 뒤 slug 를 바꾸면
아카이브 URL 이 404 가 되는데, 이 블로그에는 301 리다이렉트가 없습니다.

### (3) 사이트 설명 확인

홈의 meta description 과 RSS 채널 설명에 `BLOG_SITE_DESC` 값이 그대로 쓰입니다.
미설정이면 코드 기본값이 나가므로 검색결과에 엉뚱한 문구가 노출될 수 있습니다.

```bash
sudo systemctl show tomcat -p Environment | tr ' ' '\n' | grep BLOG_
```

미설정이라면 아래 §1 과 함께 설정하세요.

---

## 1. 환경변수 설정

`/etc/systemd/system/tomcat.service` 의 `[Service]` 에 추가합니다.

| 변수 | 용도 | 필수 |
|---|---|---|
| `BLOG_SITE_DESC` | 홈 meta description, RSS 채널 설명 | 권장 |
| `BLOG_GOOGLE_SITE_VERIFICATION` | 구글 소유확인 토큰 | 등록 시 |
| `BLOG_NAVER_SITE_VERIFICATION` | 네이버 소유확인 토큰 | 등록 시 |
| `BLOG_DEFAULT_OG_IMAGE` | 대표 이미지 없는 글의 og:image (1200x630 권장) | 선택 |
| `BLOG_LOGO_URL` | JSON-LD publisher 로고 (최소 112px) | 선택 |

예시:

```ini
Environment="BLOG_SITE_DESC=이메일 발송과 도달률, SPF·DKIM·DMARC 인증을 다루는 메일링크 기술 블로그입니다."
Environment="BLOG_GOOGLE_SITE_VERIFICATION=여기에_구글_토큰"
Environment="BLOG_NAVER_SITE_VERIFICATION=여기에_네이버_토큰"
```

```bash
sudo systemctl daemon-reload && sudo systemctl restart tomcat
```

토큰이 비어 있으면 해당 meta 태그를 아예 출력하지 않습니다. 빈 태그가 남지 않습니다.

**주의**: `BLOG_DEFAULT_OG_IMAGE` 와 `BLOG_LOGO_URL` 은 미설정이 기본입니다.
파비콘용 로고(50x37)는 두 용도 모두에 너무 작아, 잘못 넣으면
구글이 "로고가 작다" 경고를 냅니다. 제대로 된 이미지를 만든 뒤 설정하세요.

---

## 2. Google Search Console 등록

1. https://search.google.com/search-console 접속
2. 속성 추가 → **URL 접두어** 선택 → `https://tech.maillink.co.kr` 입력
   - 도메인 속성(DNS 방식)을 골라도 됩니다. 그 경우 TXT 레코드를 추가하며,
     서브도메인 전체가 한 속성으로 묶입니다.
3. 소유권 확인 → **HTML 태그** 방식 선택
4. 화면에 나오는 `content="..."` 값만 복사
   (태그 전체가 아니라 따옴표 안의 토큰만)
5. `BLOG_GOOGLE_SITE_VERIFICATION` 에 넣고 Tomcat 재시작
6. 홈에 태그가 실제로 나가는지 확인

   ```bash
   curl -s https://tech.maillink.co.kr/ | grep -i "google-site-verification"
   ```

7. Search Console 로 돌아가 **확인** 클릭

### sitemap 제출

확인이 끝나면 좌측 **Sitemaps** 메뉴에서 `sitemap.xml` 을 제출합니다.

```
https://tech.maillink.co.kr/sitemap.xml
```

`robots.txt` 에 이미 Sitemap 지시자가 있어 자동 발견도 되지만,
직접 제출하면 처리 상태를 화면에서 볼 수 있습니다.

### 색인 요청

**URL 검사** 에 개별 글 주소를 넣고 "색인 생성 요청" 을 누르면 더 빨리 반영됩니다.
글 5편을 하나씩 넣어 두세요. 실제 색인까지는 며칠에서 몇 주가 걸립니다.

---

## 3. 네이버 서치어드바이저 등록

국내 B2B 대상이라면 구글만큼, 경우에 따라 그 이상 중요합니다.

1. https://searchadvisor.naver.com 접속 → 웹마스터 도구
2. 사이트 등록 → `https://tech.maillink.co.kr` 입력
3. 소유확인 → **HTML 태그** 방식 선택 → 토큰 복사
4. `BLOG_NAVER_SITE_VERIFICATION` 에 넣고 Tomcat 재시작 후 확인
5. **요청 > 사이트맵 제출** 에 `https://tech.maillink.co.kr/sitemap.xml`
6. **요청 > RSS 제출** 에 `https://tech.maillink.co.kr/rss.xml`
   (네이버는 RSS 제출을 별도로 받습니다. 구글에는 해당 없음)

---

## 4. 등록 후 점검

### 구조화 데이터

글 상세에 `BlogPosting` 과 `BreadcrumbList`, 홈에 `WebSite`(사이트 내 검색) JSON-LD 가 나갑니다.

- 리치 결과 테스트: https://search.google.com/test/rich-results
- 스키마 검증: https://validator.schema.org/

### 공유 미리보기

`BLOG_DEFAULT_OG_IMAGE` 를 설정하기 전까지는 og:image 가 없어
카카오톡·슬랙 공유 시 이미지 없이 제목과 설명만 보입니다.
이미지가 없을 때는 `twitter:card` 도 `summary` 로 낮추므로 빈 영역은 생기지 않습니다.

### 무엇을 보게 되는가

| 기간 | 기대치 |
|---|---|
| 며칠 | Search Console 에 크롤링 기록이 잡히기 시작 |
| 1~2주 | 색인 생성됨 페이지 수 증가 |
| 1~3개월 | 검색 유입 발생. 롱테일 키워드부터 잡힘 |

색인이 안 되는 페이지는 **페이지 > 색인이 생성되지 않은 이유** 에서 사유를 볼 수 있습니다.
검색 결과 페이지(`/search`)와 결과 0건 아카이브는 의도적으로 `noindex` 이므로
"noindex 태그에 의해 제외됨" 으로 잡히는 것이 정상입니다.

---

## 5. 색인되면 안 되는 것 (이미 처리됨)

| 대상 | 처리 |
|---|---|
| `/admin/*` | `robots.txt` Disallow + 페이지 `noindex` + IP 차단 |
| `/search` | 항상 `noindex, follow`, canonical 없음 |
| 결과 0건 아카이브 | `noindex, follow` (soft-404 방지) |
| 발행 글이 없는 카테고리·태그 | sitemap 에서 제외 |
