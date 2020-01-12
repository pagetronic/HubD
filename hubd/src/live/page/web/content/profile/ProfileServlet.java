/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.profile;

import live.page.web.content.notices.push.PushSubscriptions;
import live.page.web.system.Language;
import live.page.web.system.Settings;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.utils.ServletUtils;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.BaseSession;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

/**
 * Profile servelt used to login users
 */
@Api
@WebServlet(urlPatterns = {"/profile"})
public class ProfileServlet extends HttpServlet {


	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException {
		if (CookiesUtils.purge(req, resp)) {
			return;
		}
		req.setAttribute("profile_active", "profile");
		req.setRobotsIndex(false);
		req.setCanonical("/profile");
		req.setTitle(Language.get("ACCOUNT_CONNECT", req.getLng()));

		if (!req.getString("activate", "").equals("")) {
			Users user = BaseSession.activate(req, resp, req.getString("activate", ""));
			if (user != null) {
				req.setAttribute("active", "profile");
				req.setAttribute("email", user.getString("email"));
				req.setAttribute("activate", user.getString("activate"));
			} else {
				resp.sendError(401, Language.get("URL_REDIRECTION_INVALID", req.getLng()));
				return;
			}
		}

		resp.sendTemplate(req, "/profile/profile.html");

	}

	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

		//Purging cookies
		if (CookiesUtils.purge(req, resp)) {
			return;
		}

		req.setAttribute("profile_active", "profile");
		req.setAttribute("langs", Settings.LANGS_DOMAINS.toList());
		req.setRobotsIndex(false);
		req.setCanonical("/profile");
		req.setTitle(Fx.ucfirst(Language.get("USER", req.getLng())));

		req.setAttribute("user", user);

		req.setAttribute("webpushs", PushSubscriptions.listUserFollows(user, req.getString("push", null)));

		resp.sendTemplate(req, "/profile/profile.html");

	}

	@Override
	public void doGetApiPublic(ApiServletRequest req, ApiServletResponse resp) throws IOException, ServletException {
		resp.sendResponse(new Json("error", "PLEASE_LOGIN"));
	}

	@Override
	public void doGetApiAuth(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {
		resp.sendResponse(BaseSession.getUserData(user, user.scope("email")));
	}

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {
		Json rez = new Json("error", "NOT_FOUND");

		switch (data.getString("action", "")) {
			case "password":
				rez = BaseSession.password(data.getString("email", ""), data.getString("password", ""), data.getString("activate", ""));
				break;
			case "avatar":
				rez = BaseSession.avatar(user, data.getString("avatar"));
				break;
			case "tos":
				rez = BaseSession.tos(user, data.getBoolean("accept", false));
			default:
				break;
		}
		resp.sendResponse(rez);
	}

	@Override
	public void doPostApiPublic(ApiServletRequest req, ApiServletResponse resp, Json data) throws IOException {

		Json rez = new Json("error", "NOT_FOUND");

		switch (data.getString("action", "")) {
			case "login":
				rez = BaseSession.login(req, resp, data.getString("email", ""), data.getString("password", ""));
				break;
			case "register":
				rez = BaseSession.register(req, resp, data.getString("name", ""), data.getString("email", ""), data.getString("new-password", ""), data.getJson("settings"), data.getString("key", null));
				break;
			case "recover":
				rez = BaseSession.recover(req, data.getString("email", ""));
				break;
			case "consent":
				rez = CookiesUtils.consent(data.getId(), ServletUtils.realIp(req), data.getBoolean("accept", false), data.getChoice("type", "perso", "mano", "stats"));
				break;
			case "adblock":
				rez = CookiesUtils.adBlock(ServletUtils.realIp(req), data.getBoolean("accept", false));
				break;
			default:

				break;
		}
		resp.sendResponse(rez);

	}

}
