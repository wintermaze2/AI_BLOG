<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${pageTitle}"/></title>
    <%-- 검색 결과·빈 아카이브처럼 얇은 페이지는 색인에서 제외(링크는 따라가게 follow 유지) --%>
    <c:if test="${noindex}">
    <meta name="robots" content="noindex, follow">
    </c:if>
    <c:if test="${not empty metaDescription}">
    <meta name="description" content="${fn:escapeXml(metaDescription)}">
    </c:if>
    <c:if test="${not empty canonical}">
    <link rel="canonical" href="${fn:escapeXml(canonical)}">
    </c:if>

    <!-- Open Graph / Twitter -->
    <meta property="og:site_name" content="${fn:escapeXml(siteName)}">
    <meta property="og:title" content="${fn:escapeXml(pageTitle)}">
    <c:if test="${not empty metaDescription}">
    <meta property="og:description" content="${fn:escapeXml(metaDescription)}">
    </c:if>
    <c:if test="${not empty canonical}">
    <meta property="og:url" content="${fn:escapeXml(canonical)}">
    </c:if>
    <meta property="og:type" content="${not empty post ? 'article' : 'website'}">
    <%-- 글의 대표 이미지가 없으면 서블릿이 사이트 기본 이미지(BLOG_DEFAULT_OG_IMAGE)를 넣어준다.
         이미지가 아예 없으면 twitter:card 도 summary 로 낮춘다.
         큰 카드로 선언해 놓고 이미지가 없으면 공유 시 빈 영역만 남는다. --%>
    <c:choose>
        <c:when test="${not empty ogImage}">
    <meta property="og:image" content="${fn:escapeXml(ogImage)}">
    <meta name="twitter:card" content="summary_large_image">
        </c:when>
        <c:otherwise>
    <meta name="twitter:card" content="summary">
        </c:otherwise>
    </c:choose>

    <%-- 검색엔진 소유확인. 토큰이 설정된 경우에만 출력한다. --%>
    <c:if test="${not empty googleSiteVerification}">
    <meta name="google-site-verification" content="${fn:escapeXml(googleSiteVerification)}">
    </c:if>
    <c:if test="${not empty naverSiteVerification}">
    <meta name="naver-site-verification" content="${fn:escapeXml(naverSiteVerification)}">
    </c:if>

    <%-- 파비콘. 구글은 정사각형 아이콘만 검색결과에 노출하고 48의 배수를 권장한다.
         기존 로고(50x37)는 정사각형이 아니라 기본 아이콘으로 대체되고 있었다.
         .ico 와 apple-touch-icon 은 브라우저가 관례적으로 찾는 경로에도 두었다. --%>
    <link rel="icon" href="${pageContext.request.contextPath}/favicon.ico" sizes="any">
    <link rel="icon" type="image/png" sizes="192x192"
          href="${pageContext.request.contextPath}/favicon-192.png">
    <link rel="apple-touch-icon" sizes="180x180"
          href="${pageContext.request.contextPath}/apple-touch-icon.png">
    <%@ include file="/WEB-INF/views/layout/fonts.jsp" %>
    <%-- 화면 상단 메뉴에서는 뺐지만, 피드 자동 검색용 link는 유지한다(/rss.xml 정상 동작) --%>
    <link rel="alternate" type="application/rss+xml"
          title="${fn:escapeXml(siteName)} RSS" href="${baseUrl}/rss.xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<header class="site-header">
    <div class="container">
        <a class="brand" href="${pageContext.request.contextPath}/">
            <%-- alt=""(장식용): 바로 옆 텍스트가 같은 내용을 이미 읽어주므로 중복 안내를 피한다 --%>
            <img class="brand-logo" alt="" width="50" height="37"
                 src="${pageContext.request.contextPath}/static/img/logo-bg-trans.png">
            <span>${fn:escapeXml(siteName)}</span>
        </a>
        <nav>
            <form class="search-form" role="search" method="get"
                  action="${pageContext.request.contextPath}/search">
                <input type="search" name="q" value="${fn:escapeXml(query)}"
                       placeholder="검색" aria-label="글 검색" maxlength="100">
            </form>
            <a href="${pageContext.request.contextPath}/">홈</a>
            <a class="btn-maillink" href="https://www.maillink.co.kr?r=tech">메일링크 바로가기</a>
        </nav>
    </div>
</header>
<main class="container content">
