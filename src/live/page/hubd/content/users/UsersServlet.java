/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.users;

import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.Language;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.utils.Api;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;
import java.util.Collections;

@Api(scope = "users")
@WebServlet(asyncSupported = true, urlPatterns = {"/users"}, name = "UserServlet")
public class UsersServlet extends HttpServlet {

    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

        req.setAttribute("active", "users");

        req.setTitle(Language.get("USERS", req.getLng()));
        req.setAttribute("users", UsersAggregator.getUsers(req.getString("paging", null), req.getString("q", null)));
        req.setRobotsIndex(false, true);
        if (req.contains("paging") || req.contains("q")) {
            req.setBreadCrumb(Collections.singletonList(new Json("title", Language.get("USERS", req.getLng())).put("url", "/users")));
        }
        req.setCanonical("/users", "paging");
        resp.sendTemplate(req, "/users/users.html");
    }


    @Override
    public void doGetApi(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {
        resp.sendResponse(UsersAggregator.getUsers(req.getString("paging", null), req.getString("q", null)));
    }

    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

        if (user != null) {
            switch (data.getString("action")) {
                case "search":
                    resp.sendResponse(UsersAggregator.getUsers(data.getString("paging", null), data.getString("search", null)));
                    return;
                case "value":
                    resp.sendResponse(UsersAggregator.getUsers(data.getList("ids")));
                    return;
            }
        }


        if (data.getString("action").equals("search")) {
            resp.sendResponse(UsersAggregator.getUsers(req.getString("paging", null), data.getString("search", null)));
            return;
        }
        resp.setStatus(500);
        resp.sendResponse(new Json("error", "WRONG_METHOD"));
    }

}
