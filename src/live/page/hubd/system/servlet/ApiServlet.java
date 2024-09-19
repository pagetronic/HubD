/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet;

import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import live.page.hubd.system.api.ApiUtils;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.utils.Api;
import live.page.hubd.system.servlet.utils.BruteLocker;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.AuthType;
import live.page.hubd.system.sessions.BaseCookie;
import live.page.hubd.system.sessions.BaseSession;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.system.utils.Fx;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class ApiServlet extends FullServlet {


    /**
     * Redirect user to correct function on API request
     */
    @Override
    public void serviceApi(ApiServletRequest req, ApiServletResponse resp) {

        try {
            Api api = getClass().getAnnotation(Api.class);

            Users user;
            if (req.getRequestURI().equals("/token")) {
                user = null;
            } else if (api != null && req.getHeader("Authorization") != null && req.getHeader("Authorization").startsWith("Bearer")) {


                String access_token = req.getHeader("Authorization").replaceFirst(Pattern.compile("^Bearer ", Pattern.CASE_INSENSITIVE).pattern(), "");

                Json userDb = ApiUtils.getUser(access_token);

                if (userDb != null && userDb.get("app_id") != null &&
                        (userDb.getDate("expire").after(new Date()) || userDb.getDate("expire").equals(new Date()))
                ) {
                    List<String> app_scopes = userDb.getList("app_scopes");
                    List<String> scopes = userDb.getList("scopes");
                    List<String> real_scopes = new ArrayList<>();
                    if (scopes != null) {
                        scopes.forEach(scope -> {
                            if (app_scopes.contains(scope)) {
                                real_scopes.add(scope);
                            }
                        });
                    }
                    userDb.remove("app_scopes").put("scopes", real_scopes);
                    user = new Users(userDb);

                    Db.updateOne("ApiAccess", Filters.eq("_id", userDb.getString("access")), new Json("$set", new Json("access", new Date())).put("$inc", new Json("count", 1)));

                    if (!api.scope().isEmpty() && !user.scope(api.scope())) {
                        resp.setStatus(401);
                        resp.getWriter().write(new Json("error", "AUTHORIZATION_SCOPE_ERROR").toString());
                        return;
                    }

                } else if (userDb != null && userDb.getDate("expire").before(new Date())) {

                    resp.setStatus(401);
                    resp.getWriter().write(new Json("error", "EXPIRED_ACCESS_TOKEN").toString());
                    return;

                } else if (userDb != null && userDb.get("app_id") == null) {

                    resp.setStatus(401);
                    resp.getWriter().write(new Json("error", "APP_NOT_FOUND").toString());
                    return;

                } else {
                    resp.setStatus(401);
                    BruteLocker.add(req);
                    resp.getWriter().write(new Json("error", "INVALID_ACCESS_TOKEN").toString());
                    return;
                }
                req.setAuthType(AuthType.Bearer);

            } else if (req.getHeader("Authorization") != null && req.getHeader("Authorization").startsWith("Basic")) {
                //User use a plain procedure
                String[] auth = new String(Base64.getDecoder().decode(req.getHeader("Authorization").replaceFirst(Pattern.compile("^Basic ", Pattern.CASE_INSENSITIVE).pattern(), "")), StandardCharsets.UTF_8).split(":");
                user = BaseSession.getUser(req, auth[0], auth[1]);
                req.setAuthType(AuthType.Basic);
                if (user == null) {
                    resp.setStatus(401);
                    resp.getWriter().write(new Json("error", "INVALID_AUTHORIZATION").toString());
                    return;
                }

            } else if (req.getHeader("Authorization") != null) {
                //User use a cookie header procedure
                user = BaseSession.getUser(req, req.getHeader("Authorization"));
                req.setAuthType(AuthType.Session);
                if (user == null) {
                    resp.setStatus(401);
                    resp.getWriter().write(new Json("error", "INVALID_AUTHORIZATION").toString());
                    return;
                }

            } else {
                //User use a cookie http procedure
                user = BaseSession.getOrCreateUser(req, resp);
                req.setAuthType(AuthType.Cookie);
                if (user == null && !BaseSession.sessionValid(req)) {
                    BaseCookie.clearAuth(req, resp);
                    BruteLocker.add(req);
                    resp.setStatus(401);
                    resp.sendResponse(new Json("error", "INVALID_ACCESS"));
                    return;
                }
            }

            if (user != null) {
                resp.setHeader("X-User", user.getId());
            }

            if (req.getMethod().equalsIgnoreCase("POST")) {
                Json data = null;
                String contentType = req.getHeader("Content-Type");
                if (contentType == null) {
                    resp.setStatus(500);
                    resp.sendResponse(new Json("error", "NO_CONTENT_TYPE"));
                    return;
                }
                if (contentType.matches("application/json;?.*")) {
                    try (ServletInputStream payload = req.getInputStream()) {
                        data = new Json(IOUtils.toString(payload, StandardCharsets.UTF_8));
                    } catch (Exception ignore) {
                    }
                    if (data == null) {
                        resp.setStatus(500);
                        resp.sendResponse(new Json("error", "INVALID_PAYLOAD"));
                        return;
                    }
                } else if (contentType.equalsIgnoreCase("application/x-www-form-urlencoded")) {
                    if (req.getParameter("data") != null) {
                        data = new Json(req.getParameter("data"));
                    } else {
                        data = new Json();
                        for (String key : req.getParameterMap().keySet()) {
                            data.put(key, req.getParameter(key));
                        }
                    }
                } else {
                    resp.setStatus(500);
                    resp.sendResponse(new Json("error", "INVALID_CONTENT_TYPE"));
                    return;
                }

                doPostApi(req, resp, data, user);

            } else {
                doGetApi(req, resp, user);
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            Fx.log(ex.getMessage());
            try {
                resp.setStatus(500);
                resp.getWriter().write(new Json("error", "UNKNOWN").toString());
            } catch (Exception e) {
                Fx.log(e.getMessage());
            }

        }
    }


    /**
     * Do Get request on API with authenticated users
     */
    public void doGetApi(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException, ServletException {
        resp.setStatus(404);
        resp.sendResponse(new Json("error", "METHOD_NOT_FOUND"));
    }


    /**
     * Do Post request on API with authenticated users
     */
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {
        resp.setStatus(404);
        resp.sendResponse(new Json("error", "METHOD_NOT_FOUND"));
    }

    /**
     * Go to Web service when does not match API service
     */
    @Override
    public void serviceWeb(WebServletRequest req, WebServletResponse resp) {
        try {
            resp.sendError(404, "NOT_FOUND");
        } catch (Exception ignore) {
        }
    }


    @Override
    public void destroy() {
    }
}

