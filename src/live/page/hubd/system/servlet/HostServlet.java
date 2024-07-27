/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.Settings;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.utils.GlobalControl;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;

@WebServlet(asyncSupported = true, urlPatterns = {"/STANDARD_HOST"}, displayName = "baseHost")
public class HostServlet extends GlobalControl {

    /**
     * Standard Host when site use subdomains
     */
    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {

        if (req.getRequestURI().equals("/robots.txt")) {
            resp.sendText(
                    "User-agent: *\n" +
                            "Allow: /\n" +
                            "\n" +
                            "User-agent: ia_archiver\n" +
                            "Disallow: /\n"
            );
            return;
        }

        req.setAttribute("lng", null);
        req.setUser(null);
        req.setAttribute("exchange", new Json());
        req.setRobotsIndex(false, true);
        req.setTitle(Settings.SITE_TITLE);
        resp.sendTemplate(req, "/base.html");

    }

}
