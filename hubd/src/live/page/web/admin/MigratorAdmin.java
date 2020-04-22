/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

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
				rez = migrate(user, data.getId(), data.getString("destination"));
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

		if (search != null && !search.equals("")) {
			pipeline.add(Aggregates.match(Filters.or(Filters.regex("title", Pattern.compile(search, Pattern.CASE_INSENSITIVE)), Filters.text(search))));
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

	private Json migrate(Users user, String id, String destination) {
		if (id == null || destination == null || id.equals("") || destination.equals("")) {
			return new Json("ok", false);
		}
		MongoClient destination_client = Db.getClient(Settings.MIGRATOR_DB_USER, Settings.MIGRATOR_DB_NAME, Settings.MIGRATOR_DB_PASS);
		MongoDatabase destination_base = destination_client.getDatabase(Settings.MIGRATOR_DB_NAME);
		MongoCollection<Json> destination_pages = destination_base.getCollection("Pages", Json.class);
		List<Json> pages = Db.aggregate("Pages",
				Arrays.asList(
						Aggregates.match(Filters.eq("_id", id)),
						Aggregates.limit(1),
						Aggregates.graphLookup("Pages", "$_id", "_id", "parents.0", "childs", new GraphLookupOptions().depthField("depth").maxDepth(5000)),
						Aggregates.lookup("Pages", "_id", "_id", "page"),
						Aggregates.project(new Json("branche", new Json("$concatArrays", Arrays.asList("$page", "$childs")))),
						Aggregates.unwind("$branche"),
						Aggregates.replaceRoot("$branche"),
						Aggregates.addFields(
								new Field<>("depth", new Json("$cond",
										Arrays.asList(
												new Json("$eq", Arrays.asList("$depth", new BsonUndefined())),
												-1, "$depth")
								))
						),
						Aggregates.sort(new Json("depth", 1))
				)).into(new ArrayList<>());

		Json rez = new Json("ok", true);
		for (Json page : pages) {
			if (destination_pages.countDocuments(Filters.eq("_id", page.getId()), new CountOptions().limit(1)) > 0) {
				return new Json("ok", false).put("error", "DUPLICATE_ID");
			}
		}
		for (Json page : pages) {
			Json original_page = page.clone();
			if (page.getId().equals(id)) {
				page.add("parents", destination, 0);
			}
			while (destination_pages.countDocuments(Filters.eq("url", page.getString("url")), new CountOptions().limit(1)) > 0) {
				page.put("url", page.getString("url") + "~");
			}

			destination_pages.insertOne(page);


			List<Bson> pipeline = new ArrayList<>();

			pipeline.add(Aggregates.match(Filters.eq("_id", page.getId())));
			pipeline.add(Aggregates.limit(1));
			pipeline.add(Aggregates.graphLookup("Pages", "$_id", "parents.0", "_id", "urls", new GraphLookupOptions().depthField("depth").maxDepth(50)));
			pipeline.add(Aggregates.unwind("$urls", new UnwindOptions().preserveNullAndEmptyArrays(true)));
			pipeline.add(Aggregates.sort(new Json("urls.depth", -1)));

			pipeline.add(Aggregates.group("$_id",
					Arrays.asList(
							Accumulators.first("lng", "$lng"),
							Accumulators.push("urls", "$urls.url")
					)));

			pipeline.add(Aggregates.project(new Json()
					.put("url", new Json("$reduce", new Json("input", "$urls").put("initialValue", Settings.HTTP_PROTO + Settings.MIGRATOR_LANGS_DOMAINS.getString(page.getString("lng"))).put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this"))))))
			);
			String url = destination_pages.aggregate(pipeline).first().getString("url");

			Db.save("Revisions", new Json("origine", page.getId())
					.put("editor", user.getId()).put("url", original_page.getString("url")).put("301", url).put("edit", new Date()));
			Db.deleteOne("Pages", Filters.eq("_id", original_page.getId()));
			rez.add("redirects", new Json("origin", page.getString("url")).put("destination", url));
		}
		destination_client.close();
		return rez;
	}


}
