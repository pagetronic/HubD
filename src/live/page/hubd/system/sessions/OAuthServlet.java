/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.sessions;

import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.Language;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.wrapper.*;
import live.page.hubd.system.sessions.oauth.OauthUtils;
import live.page.hubd.system.socket.SocketPusher;
import live.page.hubd.system.utils.Fx;
import org.apache.http.HttpHeaders;

import java.io.IOException;

@WebServlet(asyncSupported = true, name = "oAuthServlet", urlPatterns = {"/oauth", "/activate/*", "/logout"})
public class OAuthServlet extends HttpServlet {


    private static void logout(BaseServletRequest req, BaseServletResponse resp) throws IOException {

        resp.addHeader("Vary", HttpHeaders.REFERER);
        Json session = BaseSession.getSession(req);
        BaseCookie.clearAuth(req, resp);
        if (session != null && session.getId() != null) {
            Db.deleteOne("Sessions", Filters.eq("_id", session.getId()));
            if (session.getString("user") != null) {
                //Db.deleteMany("Sessions", Filters.eq("user", session.getString("user")));
                SocketPusher.send("user", session.getString("user"), new Json("action", "logout"));
            }

        }

        BaseCookie.clearAuth(req, resp);
    }

    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

        if (req.getQueryString() != null && req.getQueryString().matches("^(Google|Meta|WeChat).*?")) {
            OauthUtils.requestOauth(req, resp);
            return;
        }
        if (req.contains("app")) {
            OauthUtils.appRedirect(req, resp);
            return;
        }
        if (req.getParameter("code") != null || req.getParameter("oauth_verifier") != null) {
            OauthUtils.validateOauth(req, resp);
            return;

        }
        if (req.getRequestURI().equals("/logout")) {
            logout(req, resp);
            if (req.getHeader(HttpHeaders.REFERER) != null) {
                resp.sendRedirect(req.getHeader(HttpHeaders.REFERER));
            } else {
                resp.sendRedirect("/");
            }
            return;
        }
        if (req.getRequestURI().startsWith("/activate")) {
            String key = req.getRequestURI().replaceAll("^/activate/", "");
            Json userDb = Db.find("Users", Filters.and(Filters.ne("key", null), Filters.eq("key", key))).first();
            if (userDb == null) {
                resp.sendError(500, "Key invalid");
                return;
            }

            req.setUser(null);
            BaseSession.clearSession(req, resp);
            req.setAttribute("key", key);
            req.setTitle(Fx.ucfirst(Language.get("ACTIVATION_WAITING", req.getLng())));
            req.setCanonical("/profile");
            resp.sendTemplate(req, "/profile/activate.html");
            return;
        }

        resp.sendError(404);

    }

    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {

        resp.sendResponse(switch (data.getString("action", "")) {
            case "oauth_verify" -> OauthUtils.verifySessionWithCode(data.getString("code", ""), resp, req);
            case "oauth_url" -> OauthUtils.getOAuthUrl(req, resp, data.getString("provider"), data.getString("redirect"));
            default -> new Json("error", "NOT_FOUND");
        });
    }

    @Override
    public void doGetApi(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException, ServletException {
        if (req.getRequestURI().equals("/logout")) {
            logout(req, resp);
            resp.sendResponse(new Json("ok", true));
            return;
        }

        resp.sendError(404);
    }
}

