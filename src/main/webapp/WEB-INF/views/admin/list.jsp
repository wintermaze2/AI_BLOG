<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title>글 관리</title>
    <link rel="icon" type="image/png"
          href="${pageContext.request.contextPath}/static/img/logo-bg-trans.png">
    <%@ include file="/WEB-INF/views/layout/fonts.jsp" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body class="admin">
<div class="admin-wrap">
    <header class="admin-header">
        <h1>글 관리</h1>
        <div class="admin-actions">
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/admin/new">＋ 새 글</a>
            <a class="btn" href="${pageContext.request.contextPath}/" target="_blank">블로그 보기</a>
            <a class="btn" href="${pageContext.request.contextPath}/admin/logout">로그아웃</a>
        </div>
    </header>

    <c:choose>
        <c:when test="${empty posts}">
            <p class="empty">아직 글이 없습니다. “새 글”로 첫 글을 작성해 보세요.</p>
        </c:when>
        <c:otherwise>
            <table class="admin-table">
                <thead>
                <tr>
                    <th>제목</th><th>상태</th><th>카테고리</th><th>작성일</th><th>조회</th><th>관리</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="post" items="${posts}">
                    <tr>
                        <td class="col-title"><c:out value="${post.title}"/></td>
                        <td>
                            <span class="badge ${post.status == 'PUBLISHED' ? 'badge-pub' : 'badge-draft'}">
                                ${post.status == 'PUBLISHED' ? '발행' : '임시'}
                            </span>
                        </td>
                        <td><c:out value="${post.categoryName}"/></td>
                        <td>${fn:substring(post.createdAt, 0, 10)}</td>
                        <td>${post.viewCount}</td>
                        <td class="col-actions">
                            <a class="btn btn-sm" href="${pageContext.request.contextPath}/admin/edit?id=${post.id}">수정</a>
                            <form method="post" action="${pageContext.request.contextPath}/admin/delete"
                                  onsubmit="return confirm('정말 삭제하시겠습니까?');" style="display:inline">
                                <input type="hidden" name="id" value="${post.id}">
                                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                                <button type="submit" class="btn btn-sm btn-danger">삭제</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>
</body>
</html>
