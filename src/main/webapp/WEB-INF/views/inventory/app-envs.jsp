<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@taglib prefix="s" uri="http://jlab.org/jsp/smoothness"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>
<c:set value="App Envs" var="title"/>
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
                        <th>App Name</th>
                        <th>Env Name</th>
                        <th>Request Service Username</th>
                        <th>Run Service Username</th>
                        <th>Hostname</th>
                        <th>Deploy Command</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${appenvList}" var="appenv">
                        <tr>
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
                                <c:out value="${appenv.hostname}"/>
                            </td>
                            <td>
                                <c:out value="${appenv.deployCommand}"/>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </section>
    </jsp:body>
</t:inventory-page>