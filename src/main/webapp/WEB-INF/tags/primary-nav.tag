<%@tag description="Primary Navigation Tag" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@taglib prefix="s" uri="http://jlab.org/jsp/smoothness"%>
<ul>
    <li${'/choose' eq currentPath ? ' class="current-primary"' : ''}><a
            href="${pageContext.request.contextPath}/choose">Choose</a></li>
    <li${'/log' eq currentPath ? ' class="current-primary"' : ''}><a
            href="${pageContext.request.contextPath}/log">Log</a></li>
    <li${fn:startsWith(currentPath, '/inventory') ? ' class="current-primary"' : ''}><a
            href="${pageContext.request.contextPath}/inventory/apps">Inventory</a></li>
    <c:if test="${pageContext.request.isUserInRole('adm-admin')}">
        <li${fn:startsWith(currentPath, '/setup') ? ' class="current-primary"' : ''}>
            <a href="${pageContext.request.contextPath}/setup/settings">Setup</a>
        </li>
    </c:if>
    <li${'/help' eq currentPath ? ' class="current-primary"' : ''}><a
            href="${pageContext.request.contextPath}/help">Help</a></li>
</ul>