<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title>관리자 로그인</title>
    <link rel="icon" type="image/png"
          href="${pageContext.request.contextPath}/static/img/logo-bg-trans.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body class="admin">
<div class="admin-auth">
    <h1>관리자 로그인</h1>
    <c:if test="${not empty error}">
        <p class="form-error"><c:out value="${error}"/></p>
    </c:if>
    <form method="post" action="${pageContext.request.contextPath}/admin/login">
        <label>아이디
            <input type="text" name="username" autocomplete="username" required autofocus>
        </label>
        <label>비밀번호
            <input type="password" name="password" autocomplete="current-password" required>
        </label>
        <button type="submit" class="btn btn-primary">로그인</button>
    </form>
    <p class="admin-back"><a href="${pageContext.request.contextPath}/">← 블로그로</a></p>
</div>
</body>
</html>
