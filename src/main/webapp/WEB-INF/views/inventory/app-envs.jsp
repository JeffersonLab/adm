<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@taglib prefix="s" uri="jlab.tags.smoothness"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>
<c:set value="App Envs" var="title"/>
<t:inventory-page title="${title}">
    <jsp:attribute name="stylesheets">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/v${initParam.releaseNumber}/css/app-envs.css"/>
    </jsp:attribute>
    <jsp:attribute name="scripts">
        <script type="text/javascript" src="${pageContext.request.contextPath}/resources/v${initParam.releaseNumber}/js/app-envs.js"></script>
    </jsp:attribute>
    <jsp:body>
        <section>
            <s:filter-flyout-widget clearButton="true">
            <form id="app-envs-form" class="filter-form" method="get" action="app-envs">
                <div class="filter-form-panel">
                    <fieldset>
                        <legend>Filter</legend>
                        <ul class="key-value-list">
                            <li>
                                <div class="li-key">
                                    <label for="env-name">Env Name</label>
                                </div>
                                <div class="li-value">
                                    <input id="env-name"
                                           name="envName" value="${fn:escapeXml(param.envName)}"/>
                                    <div>(use * as wildcard)</div>
                                </div>
                            </li>
                            <li>
                                <div class="li-key">
                                    <label for="app-name">App Name</label>
                                </div>
                                <div class="li-value">
                                    <input id="app-name"
                                           name="appName" value="${fn:escapeXml(param.appName)}"/>
                                    <div>(use * as wildcard)</div>
                                </div>
                            </li>
                            <li>
                                <div class="li-key">
                                    <label for="hostname">Deploy Hostname</label>
                                </div>
                                <div class="li-value">
                                    <input id="hostname"
                                           name="hostname" value="${fn:escapeXml(param.hostname)}"/>
                                    <div>(use * as wildcard)</div>
                                </div>
                            </li>
                        </ul>
                    </fieldset>
                </div>
                <input type="hidden" class="offset-input" name="offset" value="0"/>
                <input class="filter-form-submit-button" type="submit" value="Apply"/>
            </form>
            </s:filter-flyout-widget>
            <h2 class="page-header-title"><c:out value="${title}"/></h2>
            <div class="message-box"><c:out value="${selectionMessage}"/></div>
            <c:set var="readonly" value="${!pageContext.request.isUserInRole('adm-admin')}"/>
            <c:if test="${not readonly}">
                <s:editable-row-table-controls>
                </s:editable-row-table-controls>
            </c:if>
            <table class="data-table stripped-table ${readonly ? '' : 'uniselect-table editable-row-table'}">
                <thead>
                    <tr>
                        <th>App Name</th>
                        <th>Env Name</th>
                        <th>Request Username</th>
                        <th>Deploy Username</th>
                        <th>Deploy Hostname:Port</th>
                        <th>Deploy Command</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${appenvList}" var="appenv">
                        <tr
                            data-id="${appenv.appEnvId}"
                            data-app-name="${fn:escapeXml(appenv.app.name)}"
                            data-env-name="${fn:escapeXml(appenv.name)}"
                            data-request-username="${fn:escapeXml(appenv.requestServiceUsername)}"
                            data-deploy-username="${fn:escapeXml(appenv.runServiceUsername)}"
                            data-deploy-hostname="${fn:escapeXml(appenv.hostname)}"
                            data-deploy-port="${fn:escapeXml(appenv.port)}"
                            data-deploy-command="${fn:escapeXml(appenv.deployCommand)}">
                            <td>
                                <c:out value="${appenv.app.name}"/>
                            </td>
                            <td>
                                <c:out value="${appenv.name}"/>
                            </td>
                            <td>
                                <c:out value="${appenv.requestServiceUsername}"/>
                            </td>
                            <td>
                                <c:out value="${appenv.runServiceUsername}"/>
                            </td>
                            <td>
                                <c:out value="${appenv.hostname}"/>:<c:out value="${appenv.port}"/>
                            </td>
                            <td>
                                <c:out value="${appenv.deployCommand}"/>
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
                <select id="max-select" name="max" form="app-envs-form" class="change-submit">
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
                                <label for="row-app-name">App Name</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-app-name"/>
                            </div>
                        </li>
                        <li>
                            <div class="li-key">
                                <label for="row-env-name">Env Name</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-env-name"/>
                            </div>
                        </li>
                        <li>
                            <div class="li-key">
                                <label for="row-request-username">Request Username</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-request-username"/>
                            </div>
                        </li>
                        <li>
                            <div class="li-key">
                                <label for="row-deploy-username">Deploy Username</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-deploy-username"/>
                            </div>
                        </li>
                        <li>
                            <div class="li-key">
                                <label for="row-deploy-hostname">Deploy Hostname</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-deploy-hostname"/>
                            </div>
                        </li>
                        <li>
                            <div class="li-key">
                                <label for="row-deploy-port">Deploy Port</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-deploy-port"/>
                            </div>
                        </li>
                        <li>
                            <div class="li-key">
                                <label for="row-run-deploy-command">Deploy Command</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-deploy-command"/>
                            </div>
                        </li>
                    </ul>
                </form>
            </section>
        </s:editable-row-table-dialog>
    </jsp:body>
</t:inventory-page>