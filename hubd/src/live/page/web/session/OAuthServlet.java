/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.session;

import com.mongodb.client.model.Filters;
import live.page.web.db.Db;
import live.page.web.servlet.HttpServlet;
import live.page.web.servlet.wrapper.WebServletRequest;
import live.page.web.servlet.wrapper.WebServletResponse;
import live.page.web.session.oauth.OauthUtils;
import live.page.web.utils.Fx;
import live.page.web.utils.json.Json;
import live.page.web.utils.langs.Language;
import org.apache.http.HttpHeaders;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet(name = "oAuthServlet", urlPatterns = {"/oauth", "/activate/*", "/logout"})
public class OAuthServlet extends HttpServlet {

	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException {

		if (req.getQueryString() != null && req.getQueryString().matches("^(Google|Facebook|Twitter|Live)")) {
			if (req.getUser() != null) {
				logout(req, resp);
				resp.sendTemplate(req, "/profile/profile.html");
				return;
			}
			OauthUtils.requestOauth(req, resp);

		} else if (req.getParameter("code") != null || req.getParameter("oauth_verifier") != null) {
			OauthUtils.validateOauth(req, resp);

		} else if (req.getRequestURI().equals("/logout")) {
			logout(req, resp);
			if (req.getHeader(HttpHeaders.REFERER) != null) {
				resp.sendRedirect(req.getHeader(HttpHeaders.REFERER));
			} else {
				resp.sendRedirect("/");
			}

		} else if (req.getRequestURI().startsWith("/activate")) {
			String key = req.getRequestURI().replaceAll("^/activate/", "");
			Json user = Db.find("Users", Filters.and(Filters.ne("key", null), Filters.eq("key", key))).first();
			if (user == null) {
				resp.sendError(500, "Key invalid");
				return;
			}

			req.setUser(null);
			BaseSession.remove(req, resp);
			req.setAttribute("key", key);
			req.setTitle(Fx.ucfirst(Language.get("ACTIVATION_WAITING", req.getLng())));
			req.setCanonical("/profile");
			resp.sendTemplate(req, "/profile/activate.html");

		} else {
			resp.sendError(404);
		}
	}

	private static void logout(WebServletRequest req, WebServletResponse resp) throws IOException {

		resp.addHeader("Vary", HttpHeaders.REFERER);
		Json session = BaseSession.getOrCreateSession(req, resp);
		BaseCookie.clearAuth(req, resp);
		if (session != null) {
			Db.deleteOne("Sessions", session);
		}
	}

}

