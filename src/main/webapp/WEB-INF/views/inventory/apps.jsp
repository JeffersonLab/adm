<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@taglib prefix="s" uri="http://jlab.org/jsp/smoothness"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>
<c:set value="Apps" var="title"/>
<t:inventory-page title="${title}">
    <jsp:attribute name="stylesheets">
    </jsp:attribute>
    <jsp:attribute name="scripts">
    </jsp:attribute>
    <jsp:body>
        <section>
            <h2 id="page-header-title"><c:out value="${title}"/></h2>
            <table class="data-table stripped-table">
                <thead>
                    <tr>
                        <th>Name</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${appList}" var="app">
                        <tr>
                            <td>
                                <c:choose>
                                    <c:when test="${empty app.docUrl}">
                                        <c:out value="${app.name}"/>
                                    </c:when>
                                    <c:otherwise>
                                        <a href="${app.docUrl}"><c:out value="${app.name}"/></a>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </section>
    </jsp:body>
</t:inventory-page>