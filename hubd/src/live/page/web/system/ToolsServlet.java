/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.system;

import live.page.web.content.search.SearchUtils;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.sessions.Users;
import live.page.web.utils.scrap.ScrapDataUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

/**
 * Servlet used for specials function, in general for transition
 */
@WebServlet(urlPatterns = {"/tools"}, displayName = "tools")
public class ToolsServlet extends HttpServlet {


	@Override
	public void doPostApiPublic(ApiServletRequest req, ApiServletResponse resp, Json data) throws IOException, ServletException {
		resp.sendResponse(new Json("error", "PLEASE_LOGIN"));
	}

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

		Json rez = new Json();

		switch (data.getString("action")) {
			case "scrap":
				rez = ScrapDataUtils.getData(data.getString("url"));
				break;
			case "search":
				rez = SearchUtils.search(data.getString("search"), data.getString("lng"), null, data.getString("paging"));
				break;
			default:
				rez.put("error", "UNKNOWN_METHOD");

		}
		resp.sendResponse(rez);
	}
}
