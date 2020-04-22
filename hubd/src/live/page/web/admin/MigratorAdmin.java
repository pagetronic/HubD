/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;
import org.bson.conversions.Bson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@WebServlet(urlPatterns = {"/admin/migrator"})
public class MigratorAdmin extends HttpServlet {


	@Override
	public void doGetEditor(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {

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
				rez = searchDestination(data.getString("search"));
				break;
			case "migrate":
				rez = migrate(data.getId(), data.getString("destination"));
				break;
		}
		resp.sendResponse(rez);
	}

	private Json searchDestination(String search) {
		MongoClient destination_client = Db.getClient(Settings.MIGRATOR_DB_USER, Settings.MIGRATOR_DB_NAME, Settings.MIGRATOR_DB_PASS);
		MongoDatabase destination_base = destination_client.getDatabase(Settings.MIGRATOR_DB_NAME);
		MongoCollection<Json> pages_collection = destination_base.getCollection("Pages", Json.class);
		int limit = 40;
		List<Bson> pipeline = new ArrayList<>();
		List<Bson> filters = new ArrayList<>();

		if (search != null && !search.equals("")) {
			filters.add(Filters.text(search));
			pipeline.add(Aggregates.match(Filters.and(filters)));
			pipeline.add(Aggregates.sort(Sorts.orderBy(Sorts.metaTextScore("score"), Sorts.ascending("_id"))));
		} else {
			pipeline.add(Aggregates.sort(Sorts.orderBy(Sorts.descending("update"), Sorts.ascending("_id"))));
		}
		pipeline.add(Aggregates.limit(limit));
		pipeline.add(Aggregates.project(new Json("_id", false).put("id", "$_id").put("title", new Json("$concat", Arrays.asList("$title", " (", "$lng", ")"))).put("update", true)));

		List<Json> pages = pages_collection.aggregate(pipeline).into(new ArrayList<>());

		destination_client.close();
		return new Json("result", pages);
	}

	// TODO migrate all childs
	private Json migrate(String id, String destination) {
		if (id == null || destination == null || id.equals("") || destination.equals("")) {
			return new Json("ok", false);
		}
		MongoClient destination_client = Db.getClient(Settings.MIGRATOR_DB_USER, Settings.MIGRATOR_DB_NAME, Settings.MIGRATOR_DB_PASS);
		MongoDatabase destination_base = destination_client.getDatabase(Settings.MIGRATOR_DB_NAME);

		return null;
	}


}
