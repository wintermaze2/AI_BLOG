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

<%-- 공유 버튼.
     페이스북/쓰레드는 링크만으로 공유되므로 JS 없이도 동작한다.
     인스타그램은 웹 공유 URL 자체가 없어 모바일 네이티브 공유로만 가능하고,
     카카오톡은 SDK 가 필요해 BLOG_KAKAO_JS_KEY 가 설정된 경우에만 노출된다. --%>
<div class="post-share" data-share
     data-url="${fn:escapeXml(canonical)}"
     data-title="${fn:escapeXml(post.title)}">
    <span class="share-label">이 글 공유하기</span>
    <div class="share-list">
        <%-- navigator.share 를 지원할 때만 share.js 가 드러낸다 --%>
        <button type="button" class="share-btn share-native" hidden>공유하기</button>
        <a class="share-btn share-fb" target="_blank" rel="noopener noreferrer"
           href="https://www.facebook.com/sharer/sharer.php?u=${shareUrlEnc}">페이스북</a>
        <a class="share-btn share-threads" target="_blank" rel="noopener noreferrer"
           href="https://www.threads.net/intent/post?text=${shareTitleEnc}%20${shareUrlEnc}">쓰레드</a>
        <c:if test="${not empty kakaoJsKey}">
        <button type="button" class="share-btn share-kakao"
                data-key="${fn:escapeXml(kakaoJsKey)}">카카오톡</button>
        </c:if>
        <button type="button" class="share-btn share-copy">링크 복사</button>
    </div>
</div>

<c:if test="${not empty kakaoJsKey}">
<%-- integrity 는 이 버전 파일에서 직접 계산한 값이다.
     버전을 올리면 해시도 반드시 다시 계산해야 한다(안 하면 로드가 차단된다).
       curl -s <url> | openssl dgst -sha384 -binary | openssl base64 -A --%>
<script src="https://t1.kakaocdn.net/kakao_js_sdk/2.8.2/kakao.min.js"
        integrity="sha384-zt/G7/KfaRQ9dT/QIkS0ujMtzouJqzuSJcXVQu50x0rl/+mD1dc70AeOejVbMD9E"
        crossorigin="anonymous"></script>
</c:if>
<script src="${pageContext.request.contextPath}/static/js/share.js" defer></script>

<p class="back-link">
    <a href="${pageContext.request.contextPath}/">← 목록으로</a>
</p>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
