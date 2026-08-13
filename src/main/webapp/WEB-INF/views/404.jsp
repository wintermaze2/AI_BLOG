<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>
<div class="notice">
    <h1>404</h1>
    <p>요청하신 페이지를 찾을 수 없습니다.</p>
    <a href="${pageContext.request.contextPath}/">홈으로 돌아가기</a>
</div>
<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
