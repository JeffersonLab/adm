package org.jlab.deployer.presentation.controller;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "Deploy", urlPatterns = {"/deploy"})
public class Deploy extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(
            Deploy.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        JsonObjectBuilder json = Json.createObjectBuilder();

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
