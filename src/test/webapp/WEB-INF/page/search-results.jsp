<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
<meta content="text/html; charset=UTF-8">
<title>Search results</title>
</head>
<body>
  <h1>Search results</h1>
  <table border="1">
    <tr>
      <th>id</th>
      <th>name</th>
      <th>type</th>
      <th>Left zip</th>
      <th>Right zip</th>
    </tr>
    <c:choose>
      <c:when test="${empty addresses}">
        <tr>
          <td>-</td>
          <td>-</td>
          <td>-</td>
          <td>-</td>
          <td>-</td>
        </tr>     
      </c:when>
      <c:otherwise>
        <c:forEach var="address" items="${ addresses }">
          <tr>
            <td>${ address.id }</td>
            <td>${ address.name }</td>
            <td>${ address.type }</td>
            <td>${ address.leftzip }</td>
            <td>${ address.rightzip }</td>
          </tr>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </table>
</body>
</html>