<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<!-- 구조화 데이터(JSON-LD): 서블릿에서 안전하게 생성해 주입 -->
<c:if test="${not empty jsonLd}">
<script type="application/ld+json">${jsonLd}</script>
</c:if>
<c:if test="${not empty jsonLdBreadcrumb}">
<script type="application/ld+json">${jsonLdBreadcrumb}</script>
</c:if>

<article class="post-detail">
    <header class="post-header">
        <h1><c:out value="${post.title}"/></h1>
        <div class="post-meta">
            <c:if test="${not empty post.categoryName}">
                <a class="cat" href="${pageContext.request.contextPath}/category/${fn:escapeXml(post.categorySlug)}"><c:out
                        value="${post.categoryName}"/></a> ·
            </c:if>
            <time datetime="${fn:substring(post.publishedAt, 0, 10)}">
                ${fn:substring(post.publishedAt, 0, 10)}
            </time>
            · 조회 ${post.viewCount}
        </div>
    </header>

    <c:if test="${not empty post.thumbnailUrl}">
        <img class="post-thumb" src="${fn:escapeXml(post.thumbnailUrl)}"
             alt="${fn:escapeXml(post.title)}" loading="lazy">
    </c:if>

    <%-- 본문은 관리자가 작성한 신뢰된 HTML(content_html)이므로 그대로 렌더링 --%>
    <div class="post-body">
        ${post.contentHtml}
    </div>

    <c:if test="${not empty post.tags}">
        <ul class="post-tags">
            <c:forEach var="tag" items="${post.tags}">
                <li>
                    <a href="${pageContext.request.contextPath}/tag/${fn:escapeXml(tag.slug)}">#<c:out
                            value="${tag.name}"/></a>
                </li>
            </c:forEach>
        </ul>
    </c:if>
</article>

<p class="back-link">
    <a href="${pageContext.request.contextPath}/">← 목록으로</a>
</p>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
