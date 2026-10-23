# 작업 지침

메일링크 기술 블로그(`tech.maillink.co.kr`)의 코드와 콘텐츠 원고를 함께 담은 저장소입니다.

## 문서 역할

중복해서 적지 않습니다. 필요한 곳을 보세요.

| 문서 | 내용 |
|---|---|
| [README.md](README.md) | 빌드·배포·관리자 계정 생성 |
| [PROJECT_NOTES.md](PROJECT_NOTES.md) | 현재 구조·규약·설계 결정 |
| [CHANGELOG.md](CHANGELOG.md) | 날짜순 작업 기록과 운영 반영 상태 |
| [SEO.md](SEO.md) | 환경변수, 검색엔진 등록, 카테고리 현황 |
| [contents/guide.txt](contents/guide.txt) | **본문 HTML 작성 규약의 단일 출처** |

## 콘텐츠 작업

### 흐름

원고는 저장소에, 발행은 관리자 화면에서 합니다. **두 곳이 분리되어 있습니다.**

```
contents/<슬러그>.html 작성 → 커밋 → 관리자에서 본문 불러오기 → 발행
```

- 파일명과 슬러그를 일치시킵니다.
- 발행된 글의 본문을 수정하면 **관리자에서 다시 불러와야** 반영됩니다. 재배포와 무관합니다.
- 제목·요약·메타 설명·태그·카테고리는 관리자 입력 항목이라 저장소에 남지 않습니다. 본문의 사실관계를 고치면 **요약과 메타 설명도 함께 확인**하세요. 실제로 한 번 놓쳤습니다.

### 본문 규약

[contents/guide.txt](contents/guide.txt)를 따릅니다. 자주 걸리는 것만 옮겨 둡니다.

- `<h1>`·`<script>`·`<style>`·`:root`·웹폰트·`<!DOCTYPE>` 금지. 본문은 조각입니다.
- 표는 `<div class="table-wrap">`로 감쌉니다. 4열 이상은 `table-wide` + `scroll-hint`.
- `<pre><code>` 안의 `< > &`는 이스케이프합니다.
- 맺음 CTA는 필수. 링크·버튼 문구·구조는 고정이고 `p.cta-copy` 한 문장만 주제에 맞게 바꿉니다.
- 금지 표현: `낡은/낡았다` → `오래된/구형`, `고치다` → `수정하다`.
- **한글이 섞인 도식에서 아래 줄 기호를 위 줄 글자에 맞추지 마세요.** 폰트에 따라 어긋나 설명이 반대 뜻이 됩니다. 세로로 늘어놓거나 표를 쓰세요. 두 번 겪었습니다.

### 발행 전 검증

```bash
python -c "
import re
p='contents/<파일>.html'; raw=open(p,'rb').read(); s=raw.decode('utf-8')
bad=[t for t in ['p','h2','h3','ul','ol','li','div','a','strong','table','thead','tbody','td','th','tr','code','pre','blockquote'] if len(re.findall(r'<%s[ >]'%t,s))!=len(re.findall(r'</%s>'%t,s))]
print('태그균형', bad or 'OK', '| table', s.count('<table'), '/wrap', s.count('table-wrap'))
print('금지', s.count('<h1'), s.count('<script'), s.count('<style'), s.count(':root'))
for m in re.finditer(r'<pre><code>(.*?)</code></pre>', s, re.S):
    assert '<' not in m.group(1) and '>' not in m.group(1)
print('제로폭', [hex(ord(c)) for c in s if c in '​‌‍﻿'] or '없음')
print('금지표현', [w for w in ['낡','고치','고쳐'] if w in s] or '없음')"
```

CTA가 다른 글과 같은지도 봅니다. 카피 줄만 다를 수 있습니다.

```bash
diff <(sed -n '/<div class="cta">/,$p' contents/spf-dkim-dmarc-spam.html | grep -v cta-copy) \
     <(sed -n '/<div class="cta">/,$p' contents/<파일>.html | grep -v cta-copy)
```

제로폭 문자(U+200B)는 다른 편집기에서 붙여넣을 때 섞여 들어옵니다. 화면에 안 보이니 검사로만 잡힙니다.

### 내부 링크

**미발행 글로 링크하면 404입니다.** 반드시 sitemap으로 발행 여부를 확인하고 거세요.

```powershell
$x=[xml](Invoke-WebRequest 'https://tech.maillink.co.kr/sitemap.xml' -UseBasicParsing).Content
$x.urlset.url | %{ [uri]::UnescapeDataString($_.loc) } | ?{$_ -match '/posts/'}
```

### 카테고리와 태그

카테고리는 관리자에 생성 기능이 없습니다. [sql/schema.sql](sql/schema.sql)을 고치고 DB에 직접 반영합니다. 현황은 [SEO.md](SEO.md) §0 (2).

태그 규약은 [contents/guide.txt](contents/guide.txt) §20에 있습니다. 요지는 세 가지입니다.

- `메일링크`, `넷퍼씨`를 모든 글에 넣습니다.
- 홍보 태그는 주제에 맞게 — 이메일 글은 `대량메일`, 문자 글은 `대량문자`.
- 태그 이름에 띄어쓰기를 넣지 않습니다. 공백이 slug에서 하이픈이 되어 같은 개념이 둘로 갈립니다.

## 사실 확인

### 제품 기능은 추정하지 말고 물어보세요

이 저장소에서 가장 많이 틀린 부분입니다. 발신번호 글 하나에서 메일링크 동작을 **네 번** 잘못 적었습니다. 법령은 1차 출처로 검증했는데 제품 동작은 추정으로 메웠고 그게 전부 틀렸습니다.

글에 제품 기능이 들어가면 **쓰기 전에 사용자에게 확인**하세요. 그럴듯한 추론이 맞을 확률이 낮습니다.

### 법률·규격은 1차 출처로

대행사 블로그는 서로 베끼고 낡은 내용이 섞입니다. 실제로 겪은 사례입니다.

- ARC를 "Authentication-Results Chain"으로 적을 뻔했습니다. RFC 8617의 정식 명칭은 **Authenticated Received Chain**입니다.
- 친구톡이 2025년 말 종료되고 브랜드 메시지로 바뀐 것을 몰랐습니다.
- 야후의 TLS 요구는 2차 자료에만 있고 공식 문서에서 확인하지 못했습니다. 그런 경우는 글에 단서를 답니다.

1차 출처: `law.go.kr`(법령·행정규칙), `support.google.com`, `docs.kakaoi.ai`, RFC, KISA.

법령을 인용하면 참조 섹션에 원문 링크를 달고, **링크가 실제로 열리는지 조회**하세요. `law.go.kr`은 버전별 주소 대신 법령명 주소를 씁니다. 버전별은 개정 후 낡은 조문을 가리킵니다.

## 코드 작업

### 빌드와 배포

```bash
mvn -o -q package -DskipTests   # JspC 포함. JSP 오류도 여기서 잡힙니다
bash deployremote.sh            # 빌드 → 전송 → Tomcat 재배포
```

**`deployremote.sh`는 `daemon-reload`를 하지 않습니다.** 환경변수를 바꿨다면 따로 실행해야 합니다. 이걸 빠뜨려 환경변수 3개가 반영되지 않은 적이 있습니다.

```bash
sudo systemctl daemon-reload && sudo systemctl restart tomcat
```

### 루트 정적 파일

`DispatcherServlet`이 `/`를 잡으므로 WAR 루트의 정적 파일은 [web.xml](src/main/webapp/WEB-INF/web.xml)에 **경로마다 default 서블릿 매핑을 넣어야** 서빙됩니다. 파일만 두면 404입니다. `/static/*`는 이미 매핑돼 있어 그 아래는 추가 설정이 필요 없습니다.

### 배포 확인

로컬 Git Bash의 `curl`은 CA 인증서가 없어 이 사이트 조회에 실패합니다. PowerShell을 쓰세요.

```powershell
Invoke-WebRequest 'https://tech.maillink.co.kr/...' -UseBasicParsing
```

## CHANGELOG

항목 뒤에 운영 반영 상태를 답니다.

- `pending` — 코드·문서만 바뀐 상태
- `applied YYYY-MM-DD` — 운영 서버(DB 또는 WAR)까지 반영됨

반영 확인은 **실제 조회로** 하고, 무엇을 확인했는지 검증 줄에 적습니다. "재배포했다"가 아니라 "조회해서 이것을 확인했다"입니다.

## 커밋

- 저장소가 선형 히스토리를 유지합니다. 원격이 앞서 있으면 rebase하세요. 강제 푸시는 하지 않습니다.
- `git add -A`는 의도하지 않은 파일을 쓸어담습니다. 경로를 지정하세요. 한 번 실수했습니다.
- `git mv`는 자동 스테이징되므로 다른 커밋에 섞이기 쉽습니다.
- CRLF 경고는 [.gitattributes](.gitattributes)의 정규화 동작이라 정상입니다.
