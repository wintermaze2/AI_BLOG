<%--
  웹폰트 로딩 (공개/관리자 페이지 공통)

  각 화면의 <head> 안에서 정적 include 로 끌어다 쓴다:
      <%@ include file="/WEB-INF/views/layout/fonts.jsp" %>

  - Noto Sans KR : 한글 렌더링 품질 때문에 채택. 400..800 가변 축으로 요청한다.
    style.css 가 600(.cat, 라벨)과 800(.brand)도 쓰는데, 그 두께를 받아오지 않으면
    브라우저가 굵기를 합성(faux bold)해 한글이 뭉개진다.
  - JetBrains Mono : 코드 블록·본문 인라인 코드용.
  - display=swap : 폰트 다운로드 중에도 대체 글꼴로 글자를 먼저 보여준다(FOIT 방지).
  - preconnect : 폰트 CSS와 실제 폰트 파일이 서로 다른 호스트에서 오므로 둘 다 지정.
--%>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet"
      href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400..800&amp;family=JetBrains+Mono:wght@400;500&amp;display=swap">
