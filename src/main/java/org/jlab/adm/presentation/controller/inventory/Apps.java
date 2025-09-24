package org.jlab.adm.presentation.controller.inventory;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.jlab.adm.business.session.AbstractFacade;
import org.jlab.adm.business.session.AppFacade;
import org.jlab.adm.persistence.entity.App;

@WebServlet(
    name = "Apps",
    urlPatterns = {"/inventory/apps"})
public class Apps extends HttpServlet {

  @EJB AppFacade appFacade;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    List<App> appList = appFacade.findAll(new AbstractFacade.OrderDirective("name", true));

    request.setAttribute("appList", appList);

    request.getRequestDispatcher("/WEB-INF/views/inventory/apps.jsp").forward(request, response);
  }
}
