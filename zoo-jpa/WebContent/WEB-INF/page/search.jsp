<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html PUBLIC>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Animal search results</title>
</head>
<body>
  <h1>Search results</h1>
  <table border="1">
    <tr>
      <th>id</th>
      <th>name</th>
      <th>type</th>
      <th>gender</th>
      <th>talk</th>
      <th>image path</th>
    </tr>
    <c:forEach var="animal" items="${ animals }">
      <tr>
        <td>${ animal.id }</td>
        <td>${ animal.name }</td>
        <td>${ animal.type }</td>
        <td>${ animal.gender }</td>
        <td>${ animal.talk }</td>
        <td>${ animal.imagePath }</td>
      </tr>
    </c:forEach>
  </table>
</body>
</html>