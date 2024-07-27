/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.admin;

import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.StatsTools;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.utils.Api;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.utils.Fx;

import java.io.IOException;
import java.util.Date;

@Api
@WebServlet(asyncSupported = true, urlPatterns = {"/admin", "/admin/*"})
public class ServletAdmin extends HttpServlet {


    @Override
    public void doGetApi(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException, ServletException {

        if (!user.getAdmin()) {
            resp.sendResponse(new Json("error", "UNKNOWN_METHOD"));
            return;
        }

        if (req.getRequestURI().equals("/admin/stats")) {
            resp.sendResponse(new Json()
                    .put("stats", StatsTools.getSimplesStats(req.getTz()))
                    .put("urls", StatsTools.getStatsUrl(req.getTz())));
            return;
        }
        resp.sendResponse(new Json("error", "UNKNOWN_METHOD"));
    }

    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {

        if (!user.getAdmin()) {
            resp.sendResponse(new Json("error", "NOT_AUTHORIZED"));
            return;
        }


        Json rez = new Json("error", "INVALID_DATA");
        if (Fx.IS_DEBUG && req.getRequestURI().equals("/admin/langs")) {
            LangsAdmin.doPostApiAdmin(resp, data);
            return;
        }


        switch (data.getString("action")) {
            case "webpush" -> rez = WebPushAdmin.push(data);
            case "redirect" -> {
                Db.deleteMany("Revisions", Filters.eq("url", data.getString("url", "")));
                Db.save("Revisions", new Json("origine", data.getId()).put("editor", user.getId()).put("url", data.getString("url", "")).put("edit", new Date(0)));
                Db.save("Revisions", new Json("origine", data.getId()).put("editor", user.getId()).put("url", Db.findById("Pages", data.getId()).getString("url", "")).put("edit", new Date()));
                rez = new Json("ok", true);
            }
            case "remove_rating" -> {
                Db.deleteOne("Ratings", Filters.eq("_id", data.getId()));
                rez = new Json("ok", true);
            }
            case "sysLog" -> {
                Fx.log("JavaScript Log: " + data.getString("sysLog", ""));
                rez = new Json("ok", true);
            }
        }

        resp.sendResponse(rez);
    }

}
