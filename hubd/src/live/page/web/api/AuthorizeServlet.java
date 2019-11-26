/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.api;

import com.mongodb.client.model.Filters;
import live.page.web.system.db.Db;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;
import live.page.web.system.json.Json;
import live.page.web.system.Language;
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
import java.util.List;

@WebServlet(urlPatterns = {"/auth"})
public class AuthorizeServlet extends HttpServlet {

	/// auth?scope= &response_type=code &redirect_uri= &client_id= &prompt=
	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

		try {
			OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest(req);

			List<String> scopes = Scopes.sort((req.getParameterValues("scope").length > 1) ? new ArrayList<>(Arrays.asList(req.getParameterValues("scope"))) : ApiUtils.parseScope(req.getString("scope", "")));
			Json app = null;

			if (oauthRequest.getClientId() != null) {

				app = Db.find("ApiApps", Filters.eq("client_id", oauthRequest.getClientId())).first();

				if (app == null) {
					resp.sendError(500, Language.get("CLIENT_ID_NOT_EXISTS", req.getLng()));
					return;
				}

				if (app != null) {

					List<String> app_scopes = app.getList("scopes");
					if ((app_scopes == null && scopes != null) || (app_scopes != null && scopes == null) || (app_scopes != null && !app_scopes.containsAll(scopes))) {
						resp.sendError(500, Language.get("SCOPE_INVALID", req.getLng()));
						return;
					}

					String redirect_uri = oauthRequest.getRedirectURI();
					List<String> app_redirect_uri = app.getList("redirect_uri");
					boolean valid = false;
					if (app_redirect_uri != null && app_redirect_uri.size() > 0) {
						for (String redirect : app_redirect_uri) {
							if (redirect_uri.equals(redirect)) {
								valid = true;
								break;
							}
						}
					} else {
						valid = true;
					}

					if (!valid) {
						resp.sendError(500, Language.get("URL_REDIRECTION_INVALID", req.getLng()));
						return;
					}
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
			String client_id = oauthRequest.getClientId();

			Json access = Db.find("ApiAccess", Filters.and(Filters.ne("force", true), Filters.eq("user", user.getId()), Filters.eq("client_id", client_id))).first();
			if (access == null) {
				access = new Json();
			}
			access.put("code", code);
			access.put("user", user.getId());
			access.put("client_id", client_id);
			access.put("client_secret", app.getString("client_secret"));
			access.put("scopes", scopes);

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
