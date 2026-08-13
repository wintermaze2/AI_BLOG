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
    <c:if test="${not empty post and not empty post.thumbnailUrl}">
    <meta property="og:image" content="${fn:escapeXml(post.thumbnailUrl)}">
    </c:if>
    <meta name="twitter:card" content="summary_large_image">

    <link rel="alternate" type="application/rss+xml"
          title="${fn:escapeXml(siteName)} RSS" href="${baseUrl}/rss.xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<header class="site-header">
    <div class="container">
        <a class="brand" href="${pageContext.request.contextPath}/">${fn:escapeXml(siteName)}</a>
        <nav>
            <form class="search-form" role="search" method="get"
                  action="${pageContext.request.contextPath}/search">
                <input type="search" name="q" value="${fn:escapeXml(query)}"
                       placeholder="검색" aria-label="글 검색" maxlength="100">
            </form>
            <a href="${pageContext.request.contextPath}/">홈</a>
            <a href="${baseUrl}/rss.xml">RSS</a>
        </nav>
    </div>
</header>
<main class="container content">
