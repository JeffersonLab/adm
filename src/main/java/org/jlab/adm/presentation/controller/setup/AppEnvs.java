package org.jlab.adm.presentation.controller.setup;

import org.jlab.adm.business.session.AbstractFacade;
import org.jlab.adm.business.session.AppEnvFacade;
import org.jlab.adm.persistence.entity.AppEnv;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "App Envs", urlPatterns = {"/setup/app-envs"})
public class AppEnvs extends HttpServlet {

    @EJB
    AppEnvFacade appEnvFacade;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<AppEnv> appenvList = appEnvFacade.findAll(new AbstractFacade.OrderDirective("name", true));

        request.setAttribute("appenvList", appenvList);

        request.getRequestDispatcher("/WEB-INF/views/setup/app-envs.jsp").forward(request, response);
    }
}
