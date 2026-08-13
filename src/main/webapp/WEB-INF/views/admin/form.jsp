<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title>${empty post ? '새 글' : '글 수정'}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/easymde/dist/easymde.min.css">
</head>
<body class="admin">
<div class="admin-wrap">
    <header class="admin-header">
        <h1>${empty post ? '새 글 작성' : '글 수정'}</h1>
        <div class="admin-actions">
            <a class="btn" href="${pageContext.request.contextPath}/admin">← 목록</a>
        </div>
    </header>

    <form method="post" action="${pageContext.request.contextPath}/admin/save" class="post-form">
        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
        <c:if test="${not empty post}">
            <input type="hidden" name="id" value="${post.id}">
        </c:if>

        <label>제목 *
            <input type="text" name="title" required
                   value="${fn:escapeXml(post.title)}">
        </label>

        <label>Slug (URL 주소, 비우면 제목에서 자동 생성)
            <input type="text" name="slug" placeholder="예: hello-world"
                   value="${fn:escapeXml(post.slug)}">
        </label>

        <label>요약 (목록·검색 노출용)
            <input type="text" name="summary" value="${fn:escapeXml(post.summary)}">
        </label>

        <div class="form-row">
            <label>카테고리
                <select name="categoryId">
                    <option value="">(없음)</option>
                    <c:forEach var="cat" items="${categories}">
                        <option value="${cat.id}"
                            ${not empty post and post.categoryId == cat.id ? 'selected' : ''}>
                            <c:out value="${cat.name}"/>
                        </option>
                    </c:forEach>
                </select>
            </label>

            <label>상태
                <select name="status">
                    <option value="DRAFT"     ${post.status == 'PUBLISHED' ? '' : 'selected'}>임시저장</option>
                    <option value="PUBLISHED" ${post.status == 'PUBLISHED' ? 'selected' : ''}>발행</option>
                </select>
            </label>
        </div>

        <label>대표 이미지 URL (선택, og:image)
            <input type="text" name="thumbnailUrl" value="${fn:escapeXml(post.thumbnailUrl)}">
        </label>

        <label>메타 설명 (선택, 비우면 요약 사용)
            <input type="text" name="metaDescription" maxlength="320"
                   value="${fn:escapeXml(post.metaDescription)}">
        </label>

        <label>본문 (Markdown)
            <textarea id="contentMd" name="contentMd" rows="18"><c:out value="${post.contentMd}"/></textarea>
        </label>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">저장</button>
            <a class="btn" href="${pageContext.request.contextPath}/admin">취소</a>
        </div>
    </form>
</div>

<script src="https://cdn.jsdelivr.net/npm/easymde/dist/easymde.min.js"></script>
<script>
    var easyMDE = new EasyMDE({
        element: document.getElementById('contentMd'),
        spellChecker: false,
        status: false,
        autoDownloadFontAwesome: true
    });
</script>
</body>
</html>
