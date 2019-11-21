/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.coins;

import live.page.web.servlet.HttpServlet;
import live.page.web.servlet.utils.Api;
import live.page.web.servlet.wrapper.ApiServletRequest;
import live.page.web.servlet.wrapper.ApiServletResponse;
import live.page.web.session.Users;
import live.page.web.utils.json.Json;

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

