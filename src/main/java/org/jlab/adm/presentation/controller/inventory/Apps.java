package org.jlab.adm.presentation.controller.inventory;

import java.io.IOException;
import java.util.List;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
