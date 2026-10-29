<%@tag description="The Setup Page Template Tag" pageEncoding="UTF-8"%>
<%@taglib prefix="s" uri="jlab.tags.smoothness"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>
<%@attribute name="title"%>
<%@attribute name="stylesheets" fragment="true"%>
<%@attribute name="scripts" fragment="true"%>
<s:page title="${title}" category="Inventory">
    <jsp:attribute name="stylesheets">    
        <jsp:invoke fragment="stylesheets"/>
    </jsp:attribute>
    <jsp:attribute name="scripts">
        <jsp:invoke fragment="scripts"/>
    </jsp:attribute>
    <jsp:attribute name="secondaryNavigation">
        <ul>
            <li${'/inventory/app-envs' eq currentPath ? ' class="current-secondary"' : ''}><a href="${pageContext.request.contextPath}/inventory/app-envs">Deployment Allowlist</a></li>
            <li${'/inventory/apps' eq currentPath ? ' class="current-secondary"' : ''}><a href="${pageContext.request.contextPath}/inventory/apps">Apps</a></li>
        </ul>
    </jsp:attribute>
    <jsp:body>
        <jsp:doBody/>
    </jsp:body>         
</s:page>
