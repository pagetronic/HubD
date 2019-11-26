/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.users;

import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.sessions.Users;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet(urlPatterns = {"/relations"})
public class RelationsServlet extends HttpServlet {


	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {
		Json rez = new Json("error", "NOT_FOUND");

		switch (data.getString("action", "")) {
			case "search":
				rez = RelationsUtils.search(data.getString("search", ""), data.get("filter"), data.getString("paging", null), user);
				break;
			default:

				break;
		}
		resp.sendResponse(rez);

	}
}
