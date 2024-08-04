/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.StatsTools;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;

@WebServlet(asyncSupported = true, urlPatterns = {"/admin/stats"})
public class StatsServlet extends HttpServlet {


    @Override
    public void doGetApi(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException, ServletException {

        if (!user.getAdmin()) {
            resp.sendResponse(new Json("error", "UNKNOWN_METHOD"));
            return;
        }
        resp.sendResponse(new Json()
                .put("stats", StatsTools.getSimplesStats(req.getTz()))
                .put("urls", StatsTools.getStatsUrl(req.getTz())));


    }

}
