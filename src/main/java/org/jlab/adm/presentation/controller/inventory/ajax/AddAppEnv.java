package org.jlab.adm.presentation.controller.inventory.ajax;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlab.adm.business.session.AppEnvFacade;
import org.jlab.smoothness.business.exception.UserFriendlyException;
import org.jlab.smoothness.business.util.ExceptionUtil;
import org.jlab.smoothness.presentation.util.ParamConverter;

@WebServlet(
    name = "AddAppEnv",
    urlPatterns = {"/inventory/ajax/add-app-env"})
public class AddAppEnv extends HttpServlet {

  private static final Logger logger = Logger.getLogger(AddAppEnv.class.getName());

  @EJB AppEnvFacade appEnvService;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String stat = "ok";
    String error = null;
    String appName = null;
    String envName = null;

    try {
      appName = request.getParameter("appName");
      envName = request.getParameter("envName");
      String requestUsername = request.getParameter("requestUsername");
      String deployUsername = request.getParameter("deployUsername");
      String deployHostname = request.getParameter("deployHostname");
      Integer deployPort = ParamConverter.convertInteger(request, "deployPort");
      String deployCommand = request.getParameter("deployCommand");

      appEnvService.addAppEnv(
          appName,
          envName,
          requestUsername,
          deployUsername,
          deployHostname,
          deployPort,
          deployCommand);
    } catch(NumberFormatException e) {
      stat = "fail";
      error = "port must be a positive integer";
    } catch (UserFriendlyException e) {
      stat = "fail";
      error = "Unable to add App: " + e.getUserMessage();
    } catch (EJBAccessException e) {
      stat = "fail";
      error = "Unable to add App: Not authenticated / authorized (do you need to re-login?)";
    } catch (RuntimeException e) {
      stat = "fail";
      error = "Unable to add App";
      logger.log(Level.SEVERE, "Unable to add App", e);
      Throwable rootCause = ExceptionUtil.getRootCause(e);
      if ("OracleDatabaseException".equals(rootCause.getClass().getSimpleName())) {
        error = "Oracle Database Exception - make sure name doesn't already exist: " + appName;
      }
    }

    response.setContentType("application/json");

    OutputStream out = response.getOutputStream();

    try (JsonGenerator gen = Json.createGenerator(out)) {
      gen.writeStartObject().write("stat", stat);
      if (error != null) {
        gen.write("error", error);
      }
      gen.writeEnd();
    }
  }
}
