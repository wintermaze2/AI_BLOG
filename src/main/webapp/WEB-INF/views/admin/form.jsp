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

        <label>태그 (쉼표로 구분, 최대 20개)
            <input type="text" name="tags" placeholder="예: java, tomcat, seo"
                   value="${fn:escapeXml(post.tagNames)}">
        </label>

        <label>대표 이미지 URL (선택, og:image)
            <input type="text" name="thumbnailUrl" value="${fn:escapeXml(post.thumbnailUrl)}">
        </label>

        <label>메타 설명 (선택, 비우면 요약 사용)
            <input type="text" name="metaDescription" maxlength="320"
                   value="${fn:escapeXml(post.metaDescription)}">
        </label>

        <div class="form-row">
            <label>본문 형식
                <select name="contentType" id="contentType">
                    <option value="MD"   ${post.contentType == 'HTML' ? '' : 'selected'}>Markdown</option>
                    <option value="HTML" ${post.contentType == 'HTML' ? 'selected' : ''}>HTML</option>
                </select>
            </label>
            <%-- name 없음: 서버로 전송하지 않고 브라우저에서 읽어 textarea에만 채운다 --%>
            <label>파일 불러오기 (.md / .html)
                <input type="file" id="contentFile"
                       accept=".md,.markdown,.html,.htm,text/markdown,text/html">
            </label>
        </div>

        <label>본문
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
(function () {
    var textarea   = document.getElementById('contentMd');
    var typeSelect = document.getElementById('contentType');
    var fileInput  = document.getElementById('contentFile');
    var easyMDE    = null;

    // EasyMDE는 Markdown 편집기라 HTML 소스를 다루기엔 방해가 된다.
    // HTML 형식일 때는 에디터를 떼고 일반 textarea로 둔다.
    function enableMarkdownEditor() {
        if (easyMDE) return;
        easyMDE = new EasyMDE({
            element: textarea,
            spellChecker: false,
            status: false,
            autoDownloadFontAwesome: true
        });
    }

    function disableMarkdownEditor() {
        if (!easyMDE) return;
        easyMDE.toTextArea();   // 현재 내용을 textarea로 되돌리고 에디터 제거
        easyMDE = null;
    }

    function applyMode() {
        if (typeSelect.value === 'HTML') disableMarkdownEditor();
        else enableMarkdownEditor();
    }

    // 에디터가 붙어 있으면 textarea에 직접 쓰는 값은 화면에 반영되지 않으므로 분기한다.
    function setContent(text) {
        if (easyMDE) easyMDE.value(text);
        else textarea.value = text;
    }

    applyMode();
    typeSelect.addEventListener('change', applyMode);

    fileInput.addEventListener('change', function () {
        var file = fileInput.files && fileInput.files[0];
        if (!file) return;

        var reader = new FileReader();
        reader.onload = function () {
            // 확장자로 형식을 먼저 맞춘다.
            // (.html을 Markdown으로 둔 채 저장하면 flexmark가 원문을 뭉개버린다)
            var name = file.name.toLowerCase();
            if (name.endsWith('.html') || name.endsWith('.htm')) {
                typeSelect.value = 'HTML';
            } else if (name.endsWith('.md') || name.endsWith('.markdown')) {
                typeSelect.value = 'MD';
            }
            applyMode();
            setContent(reader.result);
        };
        reader.onerror = function () {
            alert('파일을 읽지 못했습니다: ' + file.name);
        };
        reader.readAsText(file, 'UTF-8');
    });
})();
</script>
</body>
</html>
