/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import live.page.web.system.Language;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;
import org.apache.commons.text.StringEscapeUtils;
import org.bson.conversions.Bson;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@WebServlet(urlPatterns = {"/reports"}, name = "Report Servlet")
public class ReportsServlet extends HttpServlet {
	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {
		if (!user.getEditor()) {
			resp.sendError(404, "Not found");
			return;
		}
		req.setTitle(Language.get("REPORTS", req.getLng()));
		req.setAttribute("active", "reports");

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.sort(Sorts.orderBy(Sorts.ascending("processed"), Sorts.descending("date"))));

		req.setAttribute("reports", Db.aggregate("Reports", pipeline));

		resp.sendTemplate(req, "/admin/reports.html");
	}

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

		Json rez = new Json("error", "INVALID_DATA");
		switch (data.getString("action", "")) {

			case "report":
				rez = report(data, user);
				break;
			case "processed":
				if (!user.getEditor()) {
					resp.sendError(401);
					return;
				}
				if (Db.updateOne("Reports", Filters.eq("_id", data.getString("id_report")), new Json("$set", new Json("processed", user.getId()))).getModifiedCount() > 0) {
					rez = new Json("ok", true);
				}
				break;

		}

		resp.sendResponse(rez);
	}

	public static Json report(Json data, Users user) {
		Db.save("Reports",
				new Json("date", new Date()).put("item", data.getString("item")).put("url", data.getString("url"))
						.put("message", StringEscapeUtils.escapeXml11(data.getText("message"))).put("user", user.getId())
		);
		return new Json("ok", true);
	}

	public static long countReports() {
		return Db.count("Reports", Filters.exists("processed", false));
	}
}
