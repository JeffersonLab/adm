package org.jlab.adm.presentation.controller;

import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlab.adm.business.session.DeployerFacade;
import org.jlab.smoothness.business.exception.UserFriendlyException;

@WebServlet(
    name = "Deploy",
    urlPatterns = {"/deploy"})
public class Deploy extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(Deploy.class.getName());

  @EJB DeployerFacade deployerFacade;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String env = request.getParameter("env");
    String app = request.getParameter("app");
    String ver = request.getParameter("ver");

    LOGGER.log(Level.INFO, "Requesting deploy of {0}, {1}, {2}", new Object[] {env, app, ver});

    String exceptionMessage = null;
    BigInteger jobId = null;

    try {
      jobId = deployerFacade.deploy(env, app, ver);
    } catch (UserFriendlyException e) {
      exceptionMessage = e.getMessage();
    } catch (IOException e) {
      // We print stack trace in this case because we don't know what exactly happened
      e.printStackTrace();
      exceptionMessage = e.getMessage();
    }

    response.setContentType("application/json");

    JsonObjectBuilder json = Json.createObjectBuilder();

    if (exceptionMessage != null) {
      json.add("exception", exceptionMessage);
    } else {
      json.add("jobId", jobId);
    }

    String jsonStr = json.build().toString();

    PrintWriter pw = response.getWriter();

    pw.write(jsonStr);

    pw.flush();

    boolean error = pw.checkError();

    if (error) {
      LOGGER.log(Level.SEVERE, "PrintWriter Error");
    }
  }
}
