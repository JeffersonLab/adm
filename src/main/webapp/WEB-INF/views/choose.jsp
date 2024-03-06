<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib prefix="s" uri="http://jlab.org/jsp/smoothness"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>
<c:set value="Choose" var="title"/>
<t:page title="${title}">
    <jsp:attribute name="stylesheets">
    </jsp:attribute>
    <jsp:attribute name="scripts">
    </jsp:attribute>
    <jsp:body>
        <section>
            <h2 id="page-header-title"><c:out value="${title}"/></h2>
            <form id="deploy-form" method="post" action="${pageContext.request.contextPath}/deploy">
                <label for="env">Environment</label>
                <input id="env" type="text" name="env" value=""/>
                <label for="app">Application</label>
                <input id="app" type="text" name="app" value=""/>
                <label for="ver">Version</label>
                <input id="ver" type="text" name="ver" value=""/>
                <button id="deploy-submit" type="submit">Deploy</button>
            </form>
            <div id="result-message"></div>
            <div id="out"></div>
            <div id="err"></div>
        </section>
    </jsp:body>
</t:page>