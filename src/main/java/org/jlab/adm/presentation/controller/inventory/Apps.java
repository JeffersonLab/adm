package org.jlab.adm.presentation.controller.inventory;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.jlab.adm.business.session.AppFacade;
import org.jlab.adm.persistence.entity.App;
import org.jlab.smoothness.presentation.util.Paginator;
import org.jlab.smoothness.presentation.util.ParamConverter;
import org.jlab.smoothness.presentation.util.ParamUtil;

@WebServlet(
    name = "Apps",
    urlPatterns = {"/inventory/apps"})
public class Apps extends HttpServlet {

  @EJB AppFacade appFacade;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    int offset = ParamUtil.convertAndValidateNonNegativeInt(request, "offset", 0);
    Integer maxPerPage = ParamConverter.convertInteger(request, "max");

    if (maxPerPage == null || maxPerPage > 100 || maxPerPage < 1) {
      maxPerPage = 10;
    }

    List<App> appList = appFacade.filterList(null, offset, maxPerPage);
    long totalRecords = appFacade.countList(null);

    Paginator paginator = new Paginator(totalRecords, offset, maxPerPage);

    String selectionMessage = createSelectionMessage(paginator, null);

    request.setAttribute("paginator", paginator);
    request.setAttribute("selectionMessage", selectionMessage);
    request.setAttribute("appList", appList);

    request.getRequestDispatcher("/WEB-INF/views/inventory/apps.jsp").forward(request, response);
  }

  private String createSelectionMessage(Paginator paginator, String appName) {
    DecimalFormat formatter = new DecimalFormat("###,###");

    String selectionMessage = "All Apps ";

    List<String> filters = new ArrayList<>();

    if (appName != null && !appName.isBlank()) {
      filters.add("Name \"" + appName + "\"");
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
