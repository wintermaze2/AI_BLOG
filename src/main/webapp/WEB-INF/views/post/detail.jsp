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
        <%-- 아이콘 전용 버튼이므로 aria-label 로 이름을 반드시 준다.
             svg 는 aria-hidden 으로 보조기술에서 감춘다. --%>
        <button type="button" class="share-btn share-native" hidden
                aria-label="공유하기" title="공유하기">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
                <circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>
                <path d="M8.6 13.5l6.8 4M15.4 6.5l-6.8 4"/>
            </svg>
        </button>
        <a class="share-btn share-fb" target="_blank" rel="noopener noreferrer"
           aria-label="페이스북으로 공유" title="페이스북"
           href="https://www.facebook.com/sharer/sharer.php?u=${shareUrlEnc}">
            <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false">
                <path d="M9.101 23.691v-7.98H6.627v-3.667h2.474v-1.58c0-4.085 1.848-5.978 5.858-5.978.401 0 .955.042 1.468.103a8.68 8.68 0 0 1 1.141.195v3.325a8.623 8.623 0 0 0-.653-.036 26.805 26.805 0 0 0-.733-.009c-.707 0-1.259.096-1.675.309a1.686 1.686 0 0 0-.679.622c-.258.42-.374.995-.374 1.752v1.297h3.919l-.386 2.103-.287 1.564h-3.246v8.245C19.396 23.238 24 18.179 24 12.044c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.628 3.874 10.35 9.101 11.647Z"/>
            </svg>
        </a>
        <a class="share-btn share-threads" target="_blank" rel="noopener noreferrer"
           aria-label="쓰레드로 공유" title="쓰레드"
           href="https://www.threads.net/intent/post?text=${shareTitleEnc}%20${shareUrlEnc}">
            <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false">
                <path d="M18.263 11.097c-.03-3.486-1.92-5.586-5.111-5.586-2.13 0-3.922.963-4.863 2.499l2.062 1.438c.535-.843 1.272-1.543 2.628-1.543 1.528 0 2.318.85 2.544 2.431a15 15 0 0 0-2.236-.173c-4.125 0-6.068 1.867-6.068 4.336s1.943 3.99 4.804 3.99c3.139 0 5.013-2.115 5.781-4.735.798.361 1.348 1.204 1.348 2.47 0 3.387-3.907 5.232-7.22 5.232-4.885 0-8.077-3.207-8.077-8.424 0-6.392 4.223-10.487 9.9-10.487 3.808 0 5.69 1.671 6.97 3.914l2.108-1.475C21.44 2.078 18.331 0 13.663 0 6.227 0 1.168 5.277 1.168 12.934c0 7 4.953 11.066 10.856 11.066 4.878 0 9.809-2.846 9.809-7.716 0-2.545-1.46-4.231-3.569-5.187m-6.33 4.855c-1.077 0-2.026-.512-2.026-1.453 0-1.483 1.822-1.934 3.606-1.934.678 0 1.34.045 1.927.173-.422 1.927-1.671 3.215-3.508 3.214Z"/>
            </svg>
        </a>
        <c:if test="${not empty kakaoJsKey}">
        <%-- 카카오는 로고 자체가 '노란 바탕 + 말풍선' 구성이라 배경색이 아이콘의 일부다 --%>
        <button type="button" class="share-btn share-kakao" data-key="${fn:escapeXml(kakaoJsKey)}"
                aria-label="카카오톡으로 공유" title="카카오톡">
            <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false">
                <ellipse cx="12" cy="10.6" rx="9" ry="7.3"/>
                <path d="M8.9 16.1 6.5 20.8c-.17.33.1.63.42.44l4.6-3.1z"/>
            </svg>
        </button>
        </c:if>
        <button type="button" class="share-btn share-copy" aria-label="링크 복사" title="링크 복사">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
                <rect x="9" y="9" width="11" height="11" rx="2"/>
                <path d="M5 15V6a2 2 0 0 1 2-2h9"/>
            </svg>
        </button>
    </div>
    <%-- 링크 복사 결과 알림. 아이콘 전용 버튼이라 버튼 글자를 바꿀 수 없다. --%>
    <span class="share-toast" role="status" aria-live="polite"></span>
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
