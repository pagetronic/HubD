/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin;

import com.mongodb.client.model.Filters;
import live.page.web.admin.scrap.ScrapAdmin;
import live.page.web.content.congrate.RatingsTools;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;
import live.page.web.system.StatsTools;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.Date;


@WebServlet(urlPatterns = {"/admin", "/admin/*"})
public class ServletAdmin extends HttpServlet {


	@Override
	public void doGetEditor(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

		req.setAttribute("active", "admin");

		if (!user.getAdmin()) {
			resp.sendError(404);
			return;
		}

		switch (req.getRequestURI()) {
			case "/admin":
				resp.sendTemplate(req, "/admin/index.html");
				break;
			case "/admin/stats":
				req.setAttribute("stats", StatsTools.getSimplesStats(req.getTz()));
				req.setAttribute("urls", StatsTools.getStatsUrl(req.getTz()));
				resp.sendTemplate(req, "/admin/stats.html");
				break;
			case "/admin/pages":
				resp.sendTemplate(req, "/admin/pages.html");
				break;
			case "/admin/301":
				resp.sendTemplate(req, "/admin/301.html");
				break;
			case "/admin/ratings":
				req.setAttribute("ratings", RatingsTools.getRatings());
				resp.sendTemplate(req, "/admin/ratings.html");
				break;
			case "/admin/langs":
				LangsAdmin.doGetEditor(req, resp);
				break;
			case "/admin/scrap":
				ScrapAdmin.doGetEditor(req, resp);
				break;
			default:
				resp.sendError(404);
		}
	}

	@Override
	public void doPostApiEditor(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

		if (!user.getAdmin()) {
			resp.sendResponse(new Json("error", "NOT_AUTHORIZED"));
			return;
		}

		Json rez = new Json("error", "INVALID_DATA");
		if (Fx.IS_DEBUG && req.getRequestURI().equals("/admin/langs")) {
			LangsAdmin.doPostApiAdmin(resp, data);
			return;
		}


		switch (data.getString("action")) {
			case "webpush":
				rez = WebPushAdmin.push(data);
				break;
			case "top_title":
				String top_title = data.getString("top_title", null);
				if (top_title == null) {
					break;
				}

				top_title = Fx.normalizePost(top_title);
				Date date = new Date();
				Json revision = new Json("edit", date).put("top_title", top_title).put("editor", user.getId()).put("origine", data.getId());
				Db.save("Revisions", revision);
				rez = new Json("ok", Db.updateOne("Pages", Filters.eq("_id", data.getId()), new Json("$set", new Json("top_title", top_title).put("update", date))).getModifiedCount() > 0);
				break;

			case "redirect":
				Db.deleteMany("Revisions", Filters.eq("url", data.getString("url", "")));
				Db.save("Revisions", new Json("origine", data.getId()).put("editor", user.getId()).put("url", data.getString("url", "")).put("edit", new Date(0)));
				Db.save("Revisions", new Json("origine", data.getId()).put("editor", user.getId()).put("url", Db.findById("Pages", data.getId()).getString("url", "")).put("edit", new Date()));
				rez = new Json("ok", true);
				break;

			case "remove_rating":
				Db.deleteOne("Ratings", Filters.eq("_id", data.getId()));
				rez = new Json("ok", true);
				break;

			case "sysLog":
				Fx.log("JavaScript Log: " + data.getString("sysLog", ""));
				rez = new Json("ok", true);
				break;

			case "scrap":
				rez = ScrapAdmin.doPostApiEditor(data);
				break;

		}

		resp.sendResponse(rez);
	}

}
