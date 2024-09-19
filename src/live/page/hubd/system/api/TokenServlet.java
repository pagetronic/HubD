/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.api;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.utils.BruteLocker;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.system.utils.Fx;

import java.io.IOException;
import java.util.Date;


/**
 * Servlet used for OAuth token
 */
@WebServlet(asyncSupported = true, urlPatterns = {"/token"})
public class TokenServlet extends HttpServlet {

    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {

        resp.setHeader("X-Robots-Tag", "noindex");

        String client_id = data.getString("client_id");
        String client_secret = data.getString("client_secret");
        String grant_type = data.getString("grant_type");
        String refreshToken = data.getString("refresh_token");
        String email = data.getString("email");
        String password = data.getString("password");


        Json app = (client_id == null || client_secret == null) ? null : Db.find("ApiApps", Filters.and(Filters.eq("client_id", client_id), Filters.eq("client_secret", client_secret))).first();
        if (app == null) {
            resp.setStatus(401);
            resp.sendResponse(new Json("error", "INVALID_APP"));
            return;
        }


        Json access;

        switch (grant_type) {
            case "refresh_token" -> {
                access = Db.find("ApiAccess", Filters.and(Filters.eq("refresh_token", refreshToken), Filters.eq("app_id", app.getId()))).first();
                if (access == null) {
                    resp.setStatus(401);
                    resp.sendResponse(new Json("error", "INVALID_REFRESH_TOKEN"));
                    return;
                }
            }
            case "authorization_code" -> {
                access = Db.findOneAndUpdate("ApiAccess", Filters.and(Filters.eq("code", data.getString("code", "__none__")), Filters.eq("app_id", app.getId())),
                        new Json("$unset", new Json("code", "").put("perishable", "")),
                        new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
                );
                if (access == null) {
                    resp.setStatus(500);
                    resp.sendResponse(new Json("error", "CODE_VERIFICATION_ERROR"));
                    return;
                }
            }
            case "password" -> {
                Json userDb = Db.find("Users", Filters.and(Filters.eq("email", email), Filters.eq("password", Fx.crypt(password)))).first();
                if (userDb == null) {
                    BruteLocker.add(req);
                    resp.setStatus(401);
                    resp.sendResponse(new Json("error", "INVALID_ACCESS"));
                    return;
                }
                access = Db.find("ApiAccess", Filters.and(Filters.eq("user", userDb.getId()), Filters.eq("app_id", app.getId()))).first();
                Date date = new Date();
                if (access == null) {
                    access = new Json();
                    access.put("date", date);
                    access.put("user", userDb.getId());
                }
                access.put("scopes", Scopes.scopes);
                access.put("expire", new Date(date.getTime() + 3600 * 1000));
                access.put("app_id", app.getId());
            }
            default -> {
                resp.setStatus(500);
                resp.sendResponse(new Json("error", "GRANT_TYPE_UNKNOWN"));
                return;
            }
        }


        Date date = new Date();
        access.put("access_token", Fx.getSecureKey());
        access.put("refresh_token", Fx.getSecureKey());

        if (!access.containsKey("date")) {
            access.put("date", date);
        }
        access.put("expire", new Date(date.getTime() + 3600 * 1000));

        if (Db.save("ApiAccess", access)) {

            resp.setStatus(200);
            resp.sendResponse(new Json()
                    .put("access_token", access.getString("access_token"))
                    .put("refresh_token", access.getString("refresh_token"))
                    .put("expires_in", 3600)
                    .put("expires_at", access.getDate("expire"))
                    .put("token_type", "Bearer"));
            return;
        }


        resp.setStatus(500);
        resp.sendResponse(new Json("error", "ERROR_UNKNOWN"));

    }

}
