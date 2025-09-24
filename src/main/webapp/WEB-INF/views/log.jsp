<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@taglib prefix="s" uri="jlab.tags.smoothness"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>
<%@taglib prefix="adm" uri="jlab.tags.adm"%>
<c:set value="Log" var="title"/>
<s:page title="${title}">
    <jsp:attribute name="stylesheets">
        <style>
            #log-content {
                white-space: pre;
                font-family: monospace;
            }
            .data-table th:first-child {
                width: 200px;
            }
            .data-table th:nth-child(2) {
                width: 200px;
            }
            .data-table th:nth-child(3) {
                width: 200px;
            }
            .data-table th:nth-child(4) {
                width: 200px;
            }
        </style>
    </jsp:attribute>
    <jsp:attribute name="scripts">
        <script>
            $(document).on('click', 'a.trace', function() {
                let $tr = $(this).closest('tr'),
                    id = $tr.find('td:first-child').text();
                    title = "Job #" + id + " - Stack Trace";
                $("#log-content").text($tr.attr("data-trace"));
                $("#view-dialog").dialog({'title': title});
                $("#view-dialog").dialog('open');
                return false;
            });
            $(document).on('click', 'a.out', function() {
                let $tr = $(this).closest('tr'),
                    id = $tr.find('td:first-child').text();
                title = "Job #" + id + " - stdout";
                $("#log-content").text($tr.attr("data-out"));
                $("#view-dialog").dialog({'title': title});
                $("#view-dialog").dialog('open');
                return false;
            });
            $(document).on('click', 'a.err', function() {
                let $tr = $(this).closest('tr'),
                    id = $tr.find('td:first-child').text();
                title = "Job #" + id + " - stderr";
                $("#log-content").text($tr.attr("data-err"));
                $("#view-dialog").dialog({'title': title});
                $("#view-dialog").dialog('open');
                return false;
            });
            $(document).on("click", ".default-clear-panel", function () {
                $("#job-id").val('');
                $("#app-name").val('');
                return false;
            });
            $(function() {
                $("#view-dialog").dialog({
                    autoOpen: false,
                    modal: true,
                    resizable: true,
                    height: "800",
                    width: "1200"
                });
            });
        </script>
    </jsp:attribute>
    <jsp:body>
        <section>
            <s:filter-flyout-widget clearButton="true" ribbon="true">
                <form class="filter-form" method="get" action="log">
                    <div class="filter-form-panel">
                        <fieldset>
                            <legend>Filter</legend>
                            <ul class="key-value-list">
                                <li>
                                    <div class="li-key">
                                        <label for="job-id">Job ID</label>
                                    </div>
                                    <div class="li-value">
                                        <input id="job-id"
                                               name="jobId" value="${fn:escapeXml(param.jobId)}"
                                               />
                                    </div>
                                </li>
                                <li>
                                    <div class="li-key">
                                        <label for="app-name">App Name</label>
                                    </div>
                                    <div class="li-value">
                                        <input id="app-name"
                                               name="appName" value="${fn:escapeXml(param.appName)}"
                                               placeholder="name"/>
                                        <div>(use % as wildcard)</div>
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
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Job #</th>
                        <th>App Env Ver</th>
                        <th>Period</th>
                        <th>Exit Code / Stack Trace</th>
                        <th>Stdout/Stderr</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="record" items="${recordList}">
                        <tr data-out="${fn:escapeXml(record.out)}" data-err="${fn:escapeXml(record.err)}" data-trace="${fn:escapeXml(record.stackTrace)}">
                            <td><c:out value="${record.deployJobId}"/></td>
                            <td><c:out value="${record.appEnv.app.name}"/> / <c:out value="${record.appEnv.name}"/> / <c:out value="${record.version}"/></td>
                            <td>
                                <fmt:formatDate value="${record.start}" pattern="dd-MMM-yyyy HH:mm:ss"/> -
                                <fmt:formatDate value="${record.end}" pattern="dd-MMM-yyyy HH:mm:ss"/>
                                <c:if test="${not empty record.end}">
                                    <c:set var="duration" value="${record.end.time - record.start.time}"/>
                                    (<c:out value="${adm:millisToHumanReadable(duration, true)}"/>)
                                </c:if>
                            </td>
                            <td>
                                <div>Exit Code:
                                    <c:choose>
                                        <c:when test="${not empty record.exitCode}">
                                            <c:out value="${record.exitCode}"/>
                                        </c:when>
                                        <c:otherwise>
                                            None
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <div>Stack Trace:
                                    <c:choose>
                                        <c:when test="${not empty record.stackTrace}">
                                            <a class="trace" href="#">View</a>
                                        </c:when>
                                        <c:otherwise>
                                            None
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </td>
                            <td>
                                <div>Stdout:
                                    <c:choose>
                                        <c:when test="${not empty record.out}">
                                            <a class="out" href="#">View</a>
                                        </c:when>
                                        <c:otherwise>
                                            None
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <div>Stderr:
                                    <c:choose>
                                        <c:when test="${not empty record.err}">
                                            <a class="err" href="#">View</a>
                                        </c:when>
                                        <c:otherwise>
                                            None
                                        </c:otherwise>
                                    </c:choose>
                                </div>
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
            <div id="view-dialog" class="dialog">
                <div id="log-content"></div>
            </div>
        </section>
    </jsp:body>
</s:page>