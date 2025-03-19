package org.jlab.adm.presentation.controller;

import java.io.IOException;
import java.util.List;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jlab.adm.business.session.AbstractFacade;
import org.jlab.adm.business.session.RemoteCommandResultFacade;
import org.jlab.adm.persistence.entity.RemoteCommandResult;

@WebServlet(
    name = "Log",
    urlPatterns = {"/log"})
public class Log extends HttpServlet {

  @EJB RemoteCommandResultFacade remoteCommandResultFacade;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    List<RemoteCommandResult> recordList =
        remoteCommandResultFacade.findAll(new AbstractFacade.OrderDirective("start", true));

    request.setAttribute("recordList", recordList);

    request.getRequestDispatcher("/WEB-INF/views/log.jsp").forward(request, response);
  }
}
