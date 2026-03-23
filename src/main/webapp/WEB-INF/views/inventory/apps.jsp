<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@taglib prefix="s" uri="jlab.tags.smoothness"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>
<c:set value="Apps" var="title"/>
<t:inventory-page title="${title}">
    <jsp:attribute name="stylesheets">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/v${initParam.releaseNumber}/css/apps.css"/>
    </jsp:attribute>
    <jsp:attribute name="scripts">
        <script type="text/javascript" src="${pageContext.request.contextPath}/resources/v${initParam.releaseNumber}/js/apps.js"></script>
    </jsp:attribute>
    <jsp:body>
        <section>
            <h2 id="page-header-title"><c:out value="${title}"/></h2>
            <c:set var="readonly" value="${!pageContext.request.isUserInRole('adm-admin')}"/>
            <c:if test="${not readonly}">
                <s:editable-row-table-controls>
                </s:editable-row-table-controls>
            </c:if>
            <table class="data-table stripped-table ${readonly ? '' : 'uniselect-table editable-row-table'}">
                <thead>
                    <tr>
                        <th>Name</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${appList}" var="app">
                        <tr data-id="${app.appId}"
                            data-name="${fn:escapeXml(app.name)}"
                            data-url="${fn:escapeXml(app.docUrl)}">
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
        <s:editable-row-table-dialog>
            <section>
                <form id="row-form">
                    <ul class="key-value-list">
                        <li>
                            <div class="li-key">
                                <label for="row-name">Name</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-name"/>
                            </div>
                        </li>
                        <li>
                            <div class="li-key">
                                <label for="row-url">Doc URL</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-url"/>
                            </div>
                        </li>
                    </ul>
                </form>
            </section>
        </s:editable-row-table-dialog>
    </jsp:body>
</t:inventory-page>