/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.users;

import live.page.web.system.Language;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.Arrays;

@Api(scope = "users")
@WebServlet(urlPatterns = {"/users"}, name = "UserServlet")
public class UsersServlet extends HttpServlet {

	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException {
		doGetAuth(req, resp, null);
	}

	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

		req.setAttribute("active", "users");

		req.setTitle(Language.get("USERS", req.getLng()));
		req.setAttribute("users", UsersAggregator.getUsers(req.getString("paging", null), req.getString("q", null), user));
		req.setRobotsIndex(false, true);
		if (req.contains("paging") || req.contains("q")) {
			req.setBreadCrumb(Arrays.asList(new Json("title", Language.get("USERS", req.getLng())).put("url", "/users")));
		}
		req.setCanonical("/users", "paging");
		resp.sendTemplate(req, "/users/users.html");
	}

	@Override
	public void doGetApiPublic(ApiServletRequest req, ApiServletResponse resp) throws IOException {
		doGetApiAuth(req, resp, null);
	}

	@Override
	public void doGetApiAuth(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {
		resp.sendResponse(UsersAggregator.getUsers(req.getString("paging", null), req.getString("q", null), user));
	}

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

		Json rez = new Json();
		switch (data.getString("action")) {

			case "search":
				rez = UsersUtils.search(data);
				break;

		}
		resp.sendResponse(rez);
	}

}
