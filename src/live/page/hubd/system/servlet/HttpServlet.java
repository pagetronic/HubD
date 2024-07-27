/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet;

import jakarta.servlet.ServletException;
import live.page.hubd.content.pages.PagesAggregator;
import live.page.hubd.system.servlet.utils.BruteLocker;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.BaseCookie;
import live.page.hubd.system.sessions.BaseSession;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.utils.Fx;

import java.io.IOException;

public class HttpServlet extends ApiServlet {


    /**
     * Redirect user to correct function
     */
    @Override
    public void serviceWeb(WebServletRequest req, WebServletResponse resp) {

        try {
            Users user = BaseSession.getOrCreateUser(req, resp);
            if (user == null && !BaseSession.sessionValid(req)) {

                BruteLocker.add(req);
                resp.setStatus(401);
                BaseCookie.clearAuth(req, resp);
                if (!Fx.IS_DEBUG) {
                    resp.setIntHeader("Refresh", BruteLocker.DELAY);
                }
                resp.getWriter().write("reload " + BruteLocker.DELAY + "s");
                return;

            }

            if (user != null && user.get("key") != null && user.get("original") == null) {
                user = null;
            }
            req.setAttribute("menu", PagesAggregator.getBase(req.getLng()));
            req.setUser(user);
            if (controlWeb(user, req, resp)) {
                doGetHttp(req, resp, user);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Fx.log(e.getMessage());
            try {
                resp.sendError(500, "error unknown");
            } catch (Exception ignore) {
            }

        }
    }

    /**
     * Do control before all, return true or false
     */
    public boolean controlWeb(Users user, WebServletRequest req, WebServletResponse resp) throws ServletException, IOException {
        return true;
    }

    /**
     * Do Get with authenticated users
     */
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {

        resp.sendError(404, "NOT_FOUND");
    }

}
