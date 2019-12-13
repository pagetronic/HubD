/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.users;

import live.page.web.content.posts.utils.ThreadsAggregator;
import live.page.web.system.Language;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Api(scope = "users")
@WebServlet(urlPatterns = {"/users/*"})
public class UserServlet extends HttpServlet {


	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {


		Json userdata = UsersAggregator.getUserData(req.getId());
		if (userdata == null) {
			resp.sendError(404, "Not found");
			return;
		}
		req.setAttribute("userdata", userdata);

		req.setAttribute("active", "users");
		req.setTitle(userdata.getString("name"));

		List<Json> breadcrumb = new ArrayList<>();
		breadcrumb.add(new Json("title", Language.get("USERS", req.getLng())).put("url", "/users"));

		req.setRobotsIndex(false, true);
		if (req.contains("paging")) {
			breadcrumb.add(new Json("title", userdata.getString("name")).put("url", "/users/" + userdata.getId()));
		}
		req.setBreadCrumb(breadcrumb);

		req.setCanonical("/users/" + userdata.getId() + (req.contains("paging") ? "?paging=" + req.getString("paging", "") : ""));

		req.setAttribute("posts", ThreadsAggregator.getUserPosts(user, userdata.getId(), req.getString("paging", null)));
		extra(req, resp);
		resp.sendTemplate(req, "/users/user.html");

	}

	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException, ServletException {
		doGetAuth(req, resp, null);
	}

	public void extra(WebServletRequest req, WebServletResponse resp) throws IOException {
	}


	@Override
	public void doGetApiAuth(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {
		resp.sendResponse(new Json()
				.put("data", UsersAggregator.getUserData(req.getId()))
				.put("posts", ThreadsAggregator.getUserPosts(user, req.getId(), req.getString("paging", null)))
		);
	}

	@Override
	public void doGetApiPublic(ApiServletRequest req, ApiServletResponse resp) throws IOException {
		doGetApiAuth(req, resp, null);
	}
}
