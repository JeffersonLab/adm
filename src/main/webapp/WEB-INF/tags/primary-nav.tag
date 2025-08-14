<%@tag description="Primary Navigation Tag" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib prefix="s" uri="http://jlab.org/jsp/smoothness"%>
<ul>
    <li${'/choose' eq currentPath ? ' class="current-primary"' : ''}><a
            href="${pageContext.request.contextPath}/choose">Choose</a></li>
    <li${'/log' eq currentPath ? ' class="current-primary"' : ''}><a
            href="${pageContext.request.contextPath}/log">Log</a></li>
    <li${fn:startsWith(currentPath, '/inventory') ? ' class="current-primary"' : ''}><a
            href="${pageContext.request.contextPath}/inventory/apps">Inventory</a></li>
    <li${'/help' eq currentPath ? ' class="current-primary"' : ''}><a
            href="${pageContext.request.contextPath}/help">Help</a></li>
</ul>