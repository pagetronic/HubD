/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet.bases;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.Settings;
import live.page.hubd.system.servlet.LightServlet;
import live.page.hubd.system.servlet.wrapper.BaseServletRequest;
import live.page.hubd.system.servlet.wrapper.BaseServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;

import java.io.IOException;

@WebServlet(asyncSupported = true, urlPatterns = {"/robots.txt"})
public class RobotsTxt extends LightServlet {


    @Override
    public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException, ServletException {

        resp.setContentType("text/plain");
        WebServletResponse.setHeaderMaxCache(resp);

        if (req.getServerName().equals(Settings.HOST_CDN) || req.getServerName().equals(Settings.HOST_API)) {

            resp.getWriter().write("User-agent: *\n" + "Allow: /\n\n" + "User-agent: ia_archiver\n" + "Disallow: /");

        } else if (Settings.domainAvailable(req.getServerName())) {

            resp.getWriter().write("User-agent: *\n" +
                    "Disallow: /users\n" +
                    "Disallow: /copyright\n" +
                    "\n" +
                    "User-agent: ia_archiver\n" +
                    "Disallow: /\n\n" +
                    "Sitemap: " + Settings.getFullHttp(req.getLng()) + "/sitemap.xml\n");

        } else if (req.getServerName().equals(Settings.STANDARD_HOST)) {

            resp.getWriter().write("User-agent: *\n" + "Allow: /\n\n" + "User-agent: ia_archiver\n" + "Disallow: /");

        } else {
            resp.getWriter().write("User-agent: *\n" + "Disallow: /\n\n");
        }

    }
}
