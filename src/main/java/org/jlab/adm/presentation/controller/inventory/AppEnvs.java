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
import org.jlab.adm.business.session.AppEnvFacade;
import org.jlab.adm.persistence.entity.AppEnv;

@WebServlet(
    name = "App Envs",
    urlPatterns = {"/inventory/app-envs"})
public class AppEnvs extends HttpServlet {

  @EJB AppEnvFacade appEnvFacade;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    List<AppEnv> appenvList = appEnvFacade.findAll(new AbstractFacade.OrderDirective("name", true));

    request.setAttribute("appenvList", appenvList);

    request
        .getRequestDispatcher("/WEB-INF/views/inventory/app-envs.jsp")
        .forward(request, response);
  }
}
