/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.profile;

import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.utils.Api;
import live.page.hubd.system.servlet.utils.ServletUtils;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.AuthType;
import live.page.hubd.system.sessions.BaseSession;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;

/**
 * Profile servelt used to login users
 */
@Api
@WebServlet(asyncSupported = true, urlPatterns = {"/profile"})
public class ProfileServlet extends HttpServlet {


    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

        req.setCanonical("/profile");
        req.setRobotsIndex(false);

        resp.sendTemplate(req, "/index.html");

    }

    @Override
    public void doGetApi(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {
        if (user == null) {
            resp.sendResponse(new Json("error", "PLEASE_LOGIN"));
            return;
        }
        resp.sendResponse(BaseSession.getUserData(user, req.authType == AuthType.Session || req.authType == AuthType.Cookie || user.scope("email")));
    }

    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

        resp.sendResponse(switch (data.getString("action", "")) {
            case "activate" -> BaseSession.activate(req, resp, data.getString("activate", ""));
            case "login" -> BaseSession.login(req, resp, data.getString("email", ""),
                    data.getString("password", ""));
            case "register" -> BaseSession.register(req, resp, data.getString("name", ""),
                    data.getString("email", ""), data.getString("password", ""),
                    data.getJson("settings"), data.getString("key", null));
            case "recover" -> BaseSession.recover(req, data.getString("email", ""));
            case "consent" -> CookiesUtils.consent(data.getId(), ServletUtils.realIp(req),
                    data.getBoolean("accept", false),
                    data.getChoice("type", "perso", "mano", "stats"));

            case "password" -> BaseSession.password(data.getString("key", ""),
                    data.getString("newPassword", ""), user);
            case "avatar" -> BaseSession.avatar(user, data.getString("avatar"));
            case "tos" -> BaseSession.tos(user, data.getBoolean("accept", false));

            default -> new Json("error", "NOT_FOUND");
        });


    }
}
