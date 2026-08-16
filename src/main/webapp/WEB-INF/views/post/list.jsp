<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%-- 홈 1페이지에서만: WebSite + 사이트 내 검색 구조화 데이터 --%>
<c:if test="${not empty jsonLd}">
<script type="application/ld+json">${jsonLd}</script>
</c:if>

<h1 class="page-title">${fn:escapeXml(heading)}</h1>

<%-- 카테고리/태그/검색 결과일 때만: 글 수 + 전체 목록으로 돌아가는 링크 --%>
<c:if test="${not empty archiveKind and (archiveKind ne 'search' or not empty query)}">
    <p class="archive-meta">
        글 ${totalCount}건
        · <a href="${pageContext.request.contextPath}/">전체 글 보기</a>
    </p>
</c:if>

<c:choose>
    <c:when test="${empty posts}">
        <p class="empty">
            <c:choose>
                <c:when test="${archiveKind eq 'search' and empty query}">
                    검색어를 입력해 주세요.
                </c:when>
                <c:when test="${archiveKind eq 'search'}">
                    '<c:out value="${query}"/>'에 대한 검색 결과가 없습니다.
                </c:when>
                <c:when test="${not empty archiveKind}">
                    이 목록에 해당하는 글이 아직 없습니다.
                </c:when>
                <c:otherwise>
                    아직 발행된 글이 없습니다.
                </c:otherwise>
            </c:choose>
        </p>
    </c:when>
    <c:otherwise>
        <ul class="post-list">
            <c:forEach var="post" items="${posts}">
                <li class="post-item">
                    <h2 class="post-item-title">
                        <a href="${pageContext.request.contextPath}/posts/${fn:escapeXml(post.slug)}">
                            <c:out value="${post.title}"/>
                        </a>
                    </h2>
                    <div class="post-meta">
                        <c:if test="${not empty post.categoryName}">
                            <a class="cat" href="${pageContext.request.contextPath}/category/${fn:escapeXml(post.categorySlug)}"><c:out
                                    value="${post.categoryName}"/></a> ·
                        </c:if>
                        <time>${fn:substring(post.publishedAt, 0, 10)}</time>
                    </div>
                    <c:if test="${not empty post.summary}">
                        <p class="post-summary"><c:out value="${post.summary}"/></p>
                    </c:if>
                </li>
            </c:forEach>
        </ul>

        <!-- 페이지네이션 -->
        <c:if test="${totalPages > 1}">
            <nav class="pagination">
                <%-- pageLinkBase는 검색어 등 유지해야 할 파라미터를 포함한다(예: "?q=tomcat&page=") --%>
                <c:if test="${page > 1}">
                    <a href="${fn:escapeXml(pageLinkBase)}${page - 1}">← 이전</a>
                </c:if>
                <span class="page-info">${page} / ${totalPages}</span>
                <c:if test="${page < totalPages}">
                    <a href="${fn:escapeXml(pageLinkBase)}${page + 1}">다음 →</a>
                </c:if>
            </nav>
        </c:if>
    </c:otherwise>
</c:choose>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
