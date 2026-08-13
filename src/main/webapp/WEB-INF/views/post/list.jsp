<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<h1 class="page-title">${fn:escapeXml(siteName)}</h1>

<c:choose>
    <c:when test="${empty posts}">
        <p class="empty">아직 발행된 글이 없습니다.</p>
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
                            <span class="cat"><c:out value="${post.categoryName}"/></span> ·
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
                <c:if test="${page > 1}">
                    <a href="?page=${page - 1}">← 이전</a>
                </c:if>
                <span class="page-info">${page} / ${totalPages}</span>
                <c:if test="${page < totalPages}">
                    <a href="?page=${page + 1}">다음 →</a>
                </c:if>
            </nav>
        </c:if>
    </c:otherwise>
</c:choose>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
