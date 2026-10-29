package org.jlab.adm.presentation.controller.inventory.ajax;

import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.jlab.adm.business.session.AppFacade;

/** App names containing the term parameter, as a JSON array, for the App autocomplete. */
@WebServlet(
    name = "SearchApps",
    urlPatterns = {"/inventory/ajax/search-apps"})
public class SearchApps extends HttpServlet {

  private static final int MAX_RESULTS = 20;

  @EJB AppFacade appService;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    List<String> names = appService.searchNames(request.getParameter("term"), MAX_RESULTS);

    response.setContentType("application/json");

    OutputStream out = response.getOutputStream();

    try (JsonGenerator gen = Json.createGenerator(out)) {
      gen.writeStartArray();
      for (String name : names) {
        gen.write(name);
      }
      gen.writeEnd();
    }
  }
}
