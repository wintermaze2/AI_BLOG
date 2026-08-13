<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>
<div class="notice">
    <h1>오류가 발생했습니다</h1>
    <p><c:out value="${errorMessage}"/></p>
    <a href="${pageContext.request.contextPath}/">홈으로 돌아가기</a>
</div>
<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
