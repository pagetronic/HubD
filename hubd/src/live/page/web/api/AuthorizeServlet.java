/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.api;

import com.mongodb.client.model.Filters;
import live.page.web.system.Language;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.issuer.UUIDValueGenerator;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Servlet used for OAuth authorization
 */
@WebServlet(urlPatterns = {"/auth"})
public class AuthorizeServlet extends HttpServlet {


	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException, ServletException {

		req.setAttribute("profile_active", "profile");
		req.setRobotsIndex(false);
		req.setCanonical("/auth");
		req.setTitle(Language.get("ACCOUNT_CONNECT", req.getLng()));
		resp.sendTemplate(req, "/profile/auth.html");

	}

	/// auth?scope= &response_type=code &redirect_uri= &client_id= &prompt=
	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

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
			if (app_scopes == null || !app_scopes.containsAll(scopes)) {
				resp.sendError(500, Language.get("SCOPE_INVALID", req.getLng()));
				return;
			}

			String redirect_uri = oauthRequest.getRedirectURI();
			if (redirect_uri != null) {
				List<String> app_redirect_uri = app.getList("redirect_uri");
				if (app_redirect_uri != null && app_redirect_uri.size() > 0 && !app_redirect_uri.contains(redirect_uri)) {
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
			String code = oauthIssuerImpl.authorizationCode();

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

			OAuthResponse oauthresp = OAuthASResponse.authorizationResponse(req, HttpServletResponse.SC_FOUND).setCode(code).location(oauthRequest.getRedirectURI()).buildQueryMessage();
			if (oauthresp.getLocationUri() != null) {
				resp.sendRedirect(oauthresp.getLocationUri());
			} else {
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
	public void doPostAuth(WebServletRequest req, WebServletResponse resp, Json data, Users user) throws IOException, ServletException {
		doGetAuth(req, resp, user);
	}
}
