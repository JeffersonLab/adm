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
            <form id="apps-form" class="filter-form" method="get" action="apps">
                <input type="hidden" class="offset-input" name="offset" value="0"/>
            </form>
            <h2 id="page-header-title"><c:out value="${title}"/></h2>
            <div class="message-box"><c:out value="${selectionMessage}"/></div>
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
            <button class="previous-button" type="button" data-offset="${paginator.previousOffset}"
                    value="Previous"${paginator.previous ? '' : ' disabled="disabled"'}>Previous
            </button>
            <button class="next-button" type="button" data-offset="${paginator.nextOffset}"
                    value="Next"${paginator.next ? '' : ' disabled="disabled"'}>Next
            </button>
            <div class="max-select">
                <label for="max-select">Max Per Page</label>
                <select id="max-select" name="max" form="apps-form" class="change-submit">
                    <option value="10"${param.max eq 10 ? ' selected="selected"' : ''}>10</option>
                    <option value="100"${param.max eq 100 ? ' selected="selected"' : ''}>100</option>
                </select>
            </div>
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