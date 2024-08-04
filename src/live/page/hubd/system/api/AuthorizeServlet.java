/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.api;

import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import live.page.hubd.system.api.oltu.as.issuer.OAuthIssuer;
import live.page.hubd.system.api.oltu.as.issuer.OAuthIssuerImpl;
import live.page.hubd.system.api.oltu.as.issuer.UUIDValueGenerator;
import live.page.hubd.system.api.oltu.as.request.OAuthAuthzRequest;
import live.page.hubd.system.api.oltu.as.response.OAuthASResponse;
import live.page.hubd.system.api.oltu.common.exception.OAuthProblemException;
import live.page.hubd.system.api.oltu.common.exception.OAuthSystemException;
import live.page.hubd.system.api.oltu.common.message.OAuthResponse;
import live.page.hubd.system.Language;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.BaseSession;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.system.utils.Fx;

import java.io.IOException;
import java.util.*;

/**
 * Servlet used for OAuth authorization
 */
@WebServlet(asyncSupported = true, urlPatterns = {"/auth"})
public class AuthorizeServlet extends HttpServlet {


    /// auth?scope= &response_type=code &redirect_uri= &client_id= &prompt=
    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {
        if (user == null) {

            req.setAttribute("profile_active", "profile");
            req.setRobotsIndex(false);
            req.setCanonical("/auth");
            req.setTitle(Language.get("ACCOUNT_CONNECT", req.getLng()));
            resp.sendTemplate(req, "/profile/auth.html");
            return;
        }

        try {
            OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest(req);

            List<String> scopes = new ArrayList<>();
            try {
                scopes = Scopes.sort((req.getParameterValues("scope").length > 1) ? new ArrayList<>(Arrays.asList(req.getParameterValues("scope"))) : ApiUtils.parseScope(req.getString("scope", "")));
            } catch (Exception ignore) {

            }
            if (oauthRequest.getClientId() == null) {
                resp.sendError(500, Language.get("CLIENT_ID_EMPTY", req.getLng()));
                return;
            }

            Json app = Db.find("ApiApps", Filters.eq("client_id", oauthRequest.getClientId())).first();

            if (app == null) {
                resp.sendError(500, Language.get("APP_UNKNOWN", req.getLng()));
                return;
            }


            List<String> app_scopes = app.getList("scopes");
            if (app_scopes == null || !new HashSet<>(app_scopes).containsAll(scopes)) {
                resp.sendError(500, Language.get("SCOPE_INVALID", req.getLng()));
                return;
            }

            String redirect_uri = oauthRequest.getRedirectURI();
            if (redirect_uri != null) {
                List<String> app_redirect_uri = app.getList("redirect_uri");
                if (app_redirect_uri != null && !app_redirect_uri.isEmpty() && !app_redirect_uri.contains(redirect_uri)) {
                    resp.sendError(500, Language.get("URL_REDIRECTION_INVALID", req.getLng()));
                    return;
                }
            }
            if (req.getParameter("secure") == null || !req.getParameter("secure").equals(req.getSessionData().getString("secure"))) {

                String secure = Fx.getSecureKey();
                req.setSessionData(new Json("secure", secure));
                req.setAttribute("secure", secure);

                req.setAttribute("appname", app.getString("name"));
                req.setAttribute("scopes", scopes);

                resp.sendTemplate(req, "/api/auth.html");
                return;

            }


            req.setSessionData(new Json());

            OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new UUIDValueGenerator());
            String code = req.getString("code", oauthIssuerImpl.authorizationCode());

            Json access = Db.find("ApiAccess", Filters.and(Filters.ne("force", true), Filters.eq("user", user.getId()), Filters.eq("app_id", app.getId()))).first();
            if (access == null) {
                access = new Json();
            }
            access.put("code", code);
            access.put("user", user.getId());
            access.put("app_id", app.getId());
            access.put("scopes", scopes);
            access.put("perishable", new Date());

            Db.save("ApiAccess", access);

            OAuthResponse oauthResp = OAuthASResponse.authorizationResponse(req, HttpServletResponse.SC_FOUND).setCode(code).location(oauthRequest.getRedirectURI()).buildQueryMessage();
            if (oauthResp.getLocationUri() != null) {
                resp.sendRedirect(oauthResp.getLocationUri());
            } else {
                if (req.contains("code")) {
                    resp.sendRedirect("/", 302);
                    return;
                }
                req.setAttribute("code", code);
                resp.sendTemplate(req, "/api/code.html");
            }

        } catch (OAuthProblemException e) {
            resp.sendError(500, "OAUTH_EXCEPTION");
        } catch (OAuthSystemException e) {
            resp.sendError(500, "OAUTH_SYSTEM_EXCEPTION");
        }
    }


    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {
        if (data.getString("action", "").equals("login")) {
            user = BaseSession.getUser(req, data.getString("email"), data.getString("password"));
            if (user != null) {
                Json session = BaseSession.buildSession(req, user.getId());
                BaseSession.sendSession(resp, session);
                resp.sendResponse(new Json("session", session.getId()));
                return;
            }
        }
        resp.sendError(401, "Error");
    }
}
