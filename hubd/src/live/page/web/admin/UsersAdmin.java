/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;
import org.bson.conversions.Bson;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@WebServlet(name = "Admin Users", urlPatterns = {"/admin/users", "/admin/users/*"})
public class UsersAdmin extends HttpServlet {

	@Override
	public void doGetEditor(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

		req.setAttribute("admin_active", "users");
		if (req.getId() != null) {
			Json user_db = Db.findById("Users", req.getId());
			req.setAttribute("user_db", user_db);
			req.setTitle(user_db.getString("name"));

			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			user_db.put("id", user_db.getId());
			user_db.remove("_id");
			req.setAttribute("user_string", gson.toJson(user_db));

			resp.sendTemplate(req, "/admin/user.html");

		} else {

			req.setAttribute("users", getUsers(req.getString("search", null), req.getString("sort", ""), req.getString("paging", null)));
			resp.sendTemplate(req, "/admin/users.html");
		}

	}


	@Override
	public void doPostApiEditor(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

		Json rez = new Json("error", "WRONG_METHOD");

		switch (data.getString("action", "")) {
			case "search":
				rez = search(data.getString("search", null), data.getString("paging", null));
				break;
		}
		resp.sendResponse(rez);
	}


	public static Json search(String search, String paging_str) {
		List<Bson> pipeline = new ArrayList<>();
		Aggregator grouper = new Aggregator("name");

		Paginer paginer = new Paginer(paging_str, "-name", 10);
		List<Bson> filters = new ArrayList<>();

		if (search != null && !search.equals("")) {
			filters.add(
					Filters.or(
							Filters.regex("_id", Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE)),
							Filters.regex("name", Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE)),
							Filters.regex("email", Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE))
					)
			);
		}
		Bson paging_filter = paginer.getFilters();

		if (paging_filter != null) {
			filters.add(paging_filter);
		}
		if (filters.size() > 0) {
			pipeline.add(Aggregates.match(Filters.and(filters)));
		}
		pipeline.add(paginer.getFirstSort());
		pipeline.add(paginer.getLimit());
		pipeline.add(paginer.getLastSort());


		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		return paginer.getResult("Users", pipeline);
	}

	public static Json getUsers(String search, String sort, String paging_str) {


		Paginer paginer = new Paginer(paging_str, sort, 20);

		List<Bson> pipeline = new ArrayList<>();
		List<Bson> filters = new ArrayList<>();

		if (search != null && !search.equals("")) {
			filters.add(
					Filters.or(
							Filters.regex("_id", Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE)),
							Filters.regex("name", Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE)),
							Filters.regex("email", Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE))
					)
			);
		}

		if (paginer.getFilters() != null) {
			filters.add(paginer.getFilters());
		}
		if (filters.size() > 0) {
			pipeline.add(Aggregates.match(Filters.and(filters)));
		}
		pipeline.add(paginer.getFirstSort());

		pipeline.add(paginer.getLimit());

		pipeline.add(paginer.getLastSort());

		return paginer.getResult("Users", pipeline);
	}
}
