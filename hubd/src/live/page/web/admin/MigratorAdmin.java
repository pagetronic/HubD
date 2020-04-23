/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin;

import live.page.web.admin.utils.MigratorUtils;
import live.page.web.system.Settings;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet(urlPatterns = {"/admin/migrator"})
public class MigratorAdmin extends HttpServlet {


	@Override
	public void doGetEditor(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {

		if (Settings.MIGRATOR_LANGS_DOMAINS.size() == 0) {
			resp.sendError(404, "No migration configured");
			return;
		}

		req.setAttribute("active", "admin");

		req.setTitle("Migrator Admin");
		req.addBreadCrumb("Admin", "/admin");
		req.setBreadCrumbTitle("Migrator Admin");

		resp.sendTemplate(req, "/admin/migrator.html");

	}


	@Override
	public void doPostApiEditor(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

		Json rez = new Json();
		switch (data.getString("action")) {
			case "search":
				rez = MigratorUtils.searchDestination(data.getString("search"));
				break;
			case "migrate":
				rez = MigratorUtils.migrate(user, data.getList("ids"), data.getString("destination"));
				break;
			case "update":
				MigratorUtils.updateRemainingTags();
				rez.put("ok", true);
				break;
			case "link":
				rez = MigratorUtils.link(data.getId(), data.getList("keywords"), user);
				break;
		}
		resp.sendResponse(rez);
	}

}
