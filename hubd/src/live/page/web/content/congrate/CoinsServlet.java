/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.congrate;

import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.sessions.Users;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@Api(scope = "threads")
@WebServlet(urlPatterns = {"/coins"})
public class CoinsServlet extends HttpServlet {

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

		Json rez = new Json("error", "INVALID_DATA");

		switch (data.getString("action")) {
			case "congrate":
				rez = CoinsUtils.congrate(user.getId(), data.getString("element", ""));
				break;
		}

		resp.sendResponse(rez);
	}

}

