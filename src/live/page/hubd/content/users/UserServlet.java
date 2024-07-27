/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.users;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.Language;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.utils.Api;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Api(scope = "users")
@WebServlet(asyncSupported = true, urlPatterns = {"/users/*"})
public class UserServlet extends HttpServlet {


    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {


        Json userdata = UsersAggregator.getUserData(req.getId());
        if (userdata == null) {
            resp.sendError(404, "NOT_FOUND");
            return;
        }
        req.setAttribute("userdata", userdata);

        req.setAttribute("active", "users");
        req.setTitle(userdata.getString("name"));

        List<Json> breadcrumb = new ArrayList<>();
        breadcrumb.add(new Json("title", Language.get("USERS", req.getLng())).put("url", "/users"));

        req.setRobotsIndex(false, true);
        if (req.contains("paging")) {
            breadcrumb.add(new Json("title", userdata.getString("name")).put("url", "/users/" + userdata.getId()));
        }
        req.setBreadCrumb(breadcrumb);

        req.setCanonical("/users/" + userdata.getId() + (req.contains("paging") ? "?paging=" + req.getString("paging", "") : ""));

        extra(req, resp);
        resp.sendTemplate(req, "/users/user.html");

    }

    public void extra(WebServletRequest req, WebServletResponse resp) throws IOException {
    }


}
