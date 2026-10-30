<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@taglib prefix="s" uri="jlab.tags.smoothness"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>
<c:set value="Deployment Allowlist" var="title"/>
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
            <p class="page-description">Each record allows its Request Username to deploy an App to an Env:
                ADM runs the Deploy Command as the Deploy Username on each Deploy Hostname.
                <a href="${pageContext.request.contextPath}/inventory/apps">Apps</a> hold what an app's envs share,
                such as its documentation URL.</p>
            <div class="message-box"><c:out value="${selectionMessage}"/></div>
            <c:set var="readonly" value="${!pageContext.request.isUserInRole('adm-admin')}"/>
            <c:if test="${not readonly}">
                <s:editable-row-table-controls>
                    <button type="button" id="open-copy-row-dialog-button" class="selected-row-action" disabled="disabled">Copy Existing</button>
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
                                <c:forEach items="${appenv.hostnames}" var="host">
                                    <div><c:out value="${host}"/>:<c:out value="${appenv.port}"/></div>
                                </c:forEach>
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
                    <div id="copy-notice">
                        New record copied from <b id="copy-source"></b>. Change its App Name or Env Name,
                        and check the usernames, hostname, port and command before saving.
                        The original record is not changed.
                    </div>
                    <ul class="key-value-list">
                        <li>
                            <div class="li-key">
                                <label for="row-app-name">App Name</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-app-name" placeholder="Search existing apps"/>
                                <div id="new-app-option">
                                    <label><input type="checkbox" id="row-new-app"/> Create new App</label>
                                </div>
                            </div>
                        </li>
                        <li class="new-app-field">
                            <div class="li-key">
                                <label for="row-doc-url">App Doc URL</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-doc-url" placeholder="Optional"/>
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
                                <%-- Presets come from settings, so no installation's accounts are built in --%>
                                <c:set var="githubUsername" value="${settings.get('GITHUB_REQUEST_USERNAME')}"/>
                                <c:set var="gitlabUsername" value="${settings.get('GITLAB_REQUEST_USERNAME')}"/>
                                <c:if test="${not empty githubUsername or not empty gitlabUsername}">
                                    <select id="row-request-preset" aria-label="Request account">
                                        <option value="" selected="selected">Custom account</option>
                                        <c:if test="${not empty githubUsername}">
                                            <option value="${fn:escapeXml(githubUsername)}">GitHub Actions</option>
                                        </c:if>
                                        <c:if test="${not empty gitlabUsername}">
                                            <option value="${fn:escapeXml(gitlabUsername)}">GitLab CI</option>
                                        </c:if>
                                    </select>
                                </c:if>
                                <input type="text" id="row-request-username" placeholder="Keycloak username"/>
                                <div class="field-hint">The account allowed to request this deploy from ADM,
                                    usually a CI service account</div>
                            </div>
                        </li>
                        <li>
                            <div class="li-key">
                                <label for="row-deploy-username">Deploy Username</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-deploy-username" placeholder="SSH username"/>
                                <div class="field-hint">The account on the Deploy Hostname that ADM logs in to over SSH
                                    to run the Deploy Command</div>
                            </div>
                        </li>
                        <li>
                            <div class="li-key">
                                <label for="row-deploy-hostname">Deploy Hostname</label>
                            </div>
                            <div class="li-value">
                                <input type="text" id="row-deploy-hostname" placeholder="host1, host2"/>
                                <div class="field-hint">One or more hosts, separated by commas.
                                    ADM runs the Deploy Command on each in turn, and stops at the first that fails</div>
                            </div>
                        </li>
                        <li>
                            <div class="li-key">
                                <label for="row-deploy-port">Deploy Port</label>
                            </div>
                            <div class="li-value">
                                <%-- The form's default, so Add and a reset start with SSH's usual port --%>
                                <input type="number" id="row-deploy-port" value="22" min="1" max="65535" step="1"/>
                            </div>
                        </li>
                        <li>
                            <div class="li-key">
                                <label for="row-deploy-command">Deploy Command</label>
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