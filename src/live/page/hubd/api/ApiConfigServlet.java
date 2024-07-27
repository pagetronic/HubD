/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.api;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.Language;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.utils.Fx;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

/**
 * Servlet who distro "apps" and "access" using OAuth functions
 */
@WebServlet(asyncSupported = true, name = "API Configuration", urlPatterns = {"/api/apps", "/api/access"})
public class ApiConfigServlet extends HttpServlet {


    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

        if (req.getRequestURI().equals("/api/apps")) {


            if (req.getString("type", "").equals("obtain") &&
                    req.getString("secure", "").equals(req.getSessionData().getString("secure"))) {

                Json app = ApiUtils.createApps(user, req.getString("name", null), req.getString("redirect_uri", null), req.getString("scopes", null), true);
                String redirect = req.getString("redirect_uri", null);
                redirect += (redirect.contains("?") ? "&" : "?") + "client_id=" + app.getString("client_id") + "&client_secret=" + app.getString("client_secret");
                resp.sendRedirect(redirect);
                return;

            } else if (req.getString("type", "").equals("obtain")) {

                String secure = Fx.getSecureKey();
                req.setSessionData(new Json("secure", secure));
                req.setAttribute("inputs", Arrays.asList(
                        new String[]{"secure", secure},
                        new String[]{"name", req.getString("name", null)},
                        new String[]{"type", req.getString("type", null)},
                        new String[]{"redirect_uri", req.getString("redirect_uri", null)},
                        new String[]{"scopes", req.getString("scopes", null)}
                ));
                req.setAttribute("redirect", req.getString("redirect_uri", "/"));
                req.setAttribute("message", Language.get("API_GET_APPS_AUTO", req.getLng(), req.getString("name", null), URI.create(req.getString("redirect_uri", "")).getHost()));

                resp.sendTemplate(req, "/confirm.html");
                return;

            }

            req.setAttribute("api_active", "apps");
            req.setTitle(Fx.ucfirst(Language.get("API_APPS", req.getLng())));
            req.setAttribute("apps", ApiUtils.getApps(user, req.getString("paging", null)));
            req.setAttribute("scopes", Scopes.scopes);
            resp.sendTemplate(req, "/api/apps.html");

        } else if (req.getRequestURI().equals("/api/access")) {

            req.setAttribute("api_active", "access");
            req.setTitle(Fx.ucfirst(Language.get("API_ACCESS", req.getLng())));

            req.setAttribute("access", ApiUtils.getAccesses(user, req.getString("paging", null)));
            resp.sendTemplate(req, "/api/access.html");

        }

    }

    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {
        if (user == null) {
            Json rez = new Json("error", "INVALID_DATA");
            switch (data.getString("action", "")) {
                case "verify":
                    rez = ApiUtils.verifyApp(data.getString("client_id", ""), data.getString("client_secret", ""), data.getString("scopes", ""));
                    break;
            }
            resp.sendResponse(rez);
            return;
        }

        new Json("error", "INVALID_DATA");
        Json rez = switch (data.getString("action", "")) {
            case "create" -> ApiUtils.createApps(user, data.getString("name"), data.getString("redirect_uri"), data.getString("scopes", ""), false);
            case "rename_apps" -> ApiUtils.renameApps(data.getId(), data.getString("name"), user);
            case "delete_apps" -> ApiUtils.deleteApps(data.getId(), user);
            case "change_secret" -> ApiUtils.changeSecret(data.getId(), user);
            case "redirect_uris" -> ApiUtils.redirectUri(data.getId(), data.getString("type"), data.getString("redirect_uri"), user);
            case "scopes" -> ApiUtils.setScopes(data.getId(), data.getList("scopes"), user);
            case "get_access" -> ApiUtils.getAccess(data.getId(), user);
            case "remove_access" -> ApiUtils.removeAccess(data.getId(), user);
            case "refresh_access" -> ApiUtils.refreshAccess(data.getId(), user);
            default -> new Json("error", "INVALID_DATA");
        };

        resp.sendResponse(rez);

    }
}
