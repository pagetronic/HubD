/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.api;

import live.page.web.system.Language;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

/**
 * Servlet who distro "apps" and "access" using OAuth functions
 */
@WebServlet(name = "API Configuration", urlPatterns = {"/api/apps", "/api/access"})
public class ApiConfigServlet extends HttpServlet {


	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

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
	public void doPostApiPublic(ApiServletRequest req, ApiServletResponse resp, Json data) throws IOException, ServletException {

		Json rez = new Json("error", "INVALID_DATA");
		switch (data.getString("action", "")) {
			case "verify":
				rez = ApiUtils.verifyApp(data.getString("client_id", ""), data.getString("client_secret", ""), data.getString("scopes", ""));
				break;
		}
		resp.sendResponse(rez);
	}

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {


		Json rez = new Json("error", "INVALID_DATA");

		switch (data.getString("action", "")) {
			case "create":
				rez = ApiUtils.createApps(user, data.getString("name"), data.getString("redirect_uri"), data.getString("scopes", ""), false);
				break;
			case "rename_apps":
				rez = ApiUtils.renameApps(data.getId(), data.getString("name"), user);
				break;
			case "delete_apps":
				rez = ApiUtils.deleteApps(data.getId(), user);
				break;
			case "change_secret":
				rez = ApiUtils.changeSecret(data.getId(), user);
				break;
			case "redirect_uris":
				rez = ApiUtils.redirectUri(data.getId(), data.getString("type"), data.getString("redirect_uri"), user);
				break;
			case "scopes":
				rez = ApiUtils.setScopes(data.getId(), data.getList("scopes"), user);
				break;
			case "get_access":
				rez = ApiUtils.getAccess(data.getId(), user);
				break;
			case "remove_access":
				rez = ApiUtils.removeAccess(data.getId(), user);
				break;
			case "refresh_access":
				rez = ApiUtils.refreshAccess(data.getId(), user);
				break;

		}

		resp.sendResponse(rez);

	}
}
