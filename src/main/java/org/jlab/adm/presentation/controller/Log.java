package org.jlab.adm.presentation.controller;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jlab.adm.business.session.DeployJobFacade;
import org.jlab.adm.persistence.entity.DeployJob;
import org.jlab.smoothness.presentation.util.Paginator;
import org.jlab.smoothness.presentation.util.ParamConverter;
import org.jlab.smoothness.presentation.util.ParamUtil;

@WebServlet(
    name = "Log",
    urlPatterns = {"/log"})
public class Log extends HttpServlet {

  @EJB DeployJobFacade deployJobFacade;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    BigInteger jobId = ParamConverter.convertBigInteger(request, "jobId");
    String appName = request.getParameter("appName");
    int offset = ParamUtil.convertAndValidateNonNegativeInt(request, "offset", 0);
    int maxPerPage = 10;

    List<DeployJob> recordList = deployJobFacade.filterList(jobId, appName, offset, maxPerPage);

    long totalRecords = deployJobFacade.countList(jobId, appName);

    Paginator paginator = new Paginator(totalRecords, offset, maxPerPage);

    String selectionMessage = createSelectionMessage(paginator, jobId, appName);

    request.setAttribute("recordList", recordList);
    request.setAttribute("selectionMessage", selectionMessage);
    request.setAttribute("paginator", paginator);

    request.getRequestDispatcher("/WEB-INF/views/log.jsp").forward(request, response);
  }

  private String createSelectionMessage(Paginator paginator, BigInteger jobId, String appName) {
    DecimalFormat formatter = new DecimalFormat("###,###");

    String selectionMessage = "All Deployment Log Records";

    List<String> filters = new ArrayList<>();

    if (jobId != null) {
      filters.add("Job ID \"" + jobId + "\"");
    }

    if (appName != null && !appName.isBlank()) {
      filters.add("App Name \"" + appName + "\"");
    }

    if (!filters.isEmpty()) {
      selectionMessage = filters.get(0);

      for (int i = 1; i < filters.size(); i++) {
        String filter = filters.get(i);
        selectionMessage += " and " + filter;
      }
    }

    if (paginator.getTotalRecords() < paginator.getMaxPerPage() && paginator.getOffset() == 0) {
      selectionMessage =
          selectionMessage + " {" + formatter.format(paginator.getTotalRecords()) + "}";
    } else {
      selectionMessage =
          selectionMessage
              + " {"
              + formatter.format(paginator.getStartNumber())
              + " - "
              + formatter.format(paginator.getEndNumber())
              + " of "
              + formatter.format(paginator.getTotalRecords())
              + "}";
    }

    return selectionMessage;
  }
}
