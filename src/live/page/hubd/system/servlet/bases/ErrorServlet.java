/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet.bases;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.Settings;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;

@WebServlet(asyncSupported = true, urlPatterns = {"/error"}, name = "error-page")
public class ErrorServlet extends HttpServlet {

    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {
        resp.setHeaderNoCache();
        req.setCanonical(null);

        if (req.getServerName().equals(Settings.STANDARD_HOST) || !Settings.domainAvailable(req.getServerName())) {

            if (!resp.isCommitted()) {
                req.getRequestDispatcher("/STANDARD_HOST").forward(req, resp);
            }

        } else if (req.getServerName().equals(Settings.STANDARD_HOST) || Settings.domainAvailable(req.getServerName())) {

            req.setAttribute("error", "Error " + resp.getStatus());
            req.setAttribute("message", req.getAttribute("jakarta.servlet.error.message"));
            resp.sendTemplate(req, "/error.html");

        } else {
            resp.sendText("Error " + resp.getStatus());
        }

    }

}
