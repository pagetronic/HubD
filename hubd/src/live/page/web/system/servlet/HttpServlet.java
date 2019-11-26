/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.servlet;

import live.page.web.system.json.Json;
import live.page.web.system.servlet.utils.BruteLocker;
import live.page.web.system.servlet.utils.ServletUtils;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.BaseCookie;
import live.page.web.system.sessions.BaseSession;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

public class HttpServlet extends ApiServlet {


	/**
	 * Redirect user to correct function
	 */
	@Override
	public void serviceWeb(WebServletRequest req, WebServletResponse resp) {

		try {

			Users user = BaseSession.getOrCreateUser(req, resp);

			if (user == null && (BaseCookie.getAuth(req) != null && !BaseSession.sessionExists(req))) {

				BaseCookie.clearAuth(req, resp);
				BruteLocker.add(ServletUtils.realIp(req));
			}

			if (user != null && user.get("key") != null && user.get("original") == null) {
				user = null;
			}

			req.setUser(user);


			if (req.getMethod().equalsIgnoreCase("POST")) {

				Json data = new Json();
				for (Map.Entry<String, String[]> para : req.getParameterMap().entrySet()) {
					data.put(para.getKey(), para.getValue()[0]);
				}

				if (user == null) {
					doPostPublic(req, resp, data);
				} else {
					doPostAuth(req, resp, data, user);
				}


			} else {

				if (user == null) {
					doGetPublic(req, resp);
				} else if (user.getEditor()) {
					doGetEditor(req, resp, user);
				} else if (controlWeb(user, req, resp)) {
					doGetAuth(req, resp, user);
				}
			}


		} catch (Exception e) {
			e.printStackTrace();
			Fx.log(e.getMessage());
			try {
				resp.sendError(500, "error unknown");
			} catch (Exception ignore) {
			}

		}
	}

	/**
	 * Do control before all, return true or false
	 */
	public boolean controlWeb(Users user, WebServletRequest req, WebServletResponse resp) throws ServletException, IOException {
		return true;
	}

	/**
	 * Do on Post with no authentication
	 */
	public void doPostPublic(WebServletRequest req, WebServletResponse resp, Json data) throws IOException, ServletException {
		resp.sendError(404);
	}

	/**
	 * Do on Post with authenticated users
	 */
	public void doPostAuth(WebServletRequest req, WebServletResponse resp, Json data, Users user) throws IOException, ServletException {
		doPostPublic(req, resp, data);
	}

	/**
	 * Do Get with no authentication
	 */
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException, ServletException {
		resp.sendError(404, "Not found");
	}

	/**
	 * Do Get with authenticated users
	 */
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {
		doGetPublic(req, resp);
	}

	/**
	 * Do Get with editor group privilege
	 */
	public void doGetEditor(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {
		if (controlWeb(user, req, resp)) {
			doGetAuth(req, resp, user);
		}
	}
}
