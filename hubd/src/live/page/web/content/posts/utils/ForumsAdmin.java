/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.posts.utils;

import com.mongodb.client.model.*;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.json.Json;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class ForumsAdmin {

	public static Json saveForum(Json forum, Users user) {
		String lng = Settings.getLang(forum.getString("domain", ""));

		Json rez = new Json();

		if (forum.getString("title", "").length() == 0) {
			rez.add("errors", new Json("title", "TOO_SHORT"));
		}
		if (forum.getString("url", "").length() == 0) {
			rez.add("errors", new Json("url", "TOO_SHORT"));
		} else if ((forum.getString("forum_id", "").equals("") &&
				Db.exists("Forums", Filters.and(Filters.eq("url.0", forum.getString("url", "")), Filters.eq("lng", lng)))) ||
				(forum.getString("forum_id", "").equals("") &&
						Db.exists("Forums", Filters.and(Filters.ne("_id", forum.getString("forum_id")),
								Filters.and(Filters.eq("url", forum.getString("url", "")), Filters.eq("lng", lng))
						)))) {
			rez.add("errors", new Json("url", "EXISTS"));
		}
		if (!rez.isEmpty()) {
			return rez;
		}
		forum.put("date", new Date()).put("user", user.getId());
		if (forum.getList("parents") == null || forum.getList("parents").size() == 0) {
			forum.put("parents", null);
		} else {
			forum.put("parents", forum.getList("parents"));
		}

		forum.put("meta_title", forum.getString("meta_title"));

		String url = Fx.cleanURL(forum.getString("url"));
		if (!forum.getString("forum_id", "").equals("")) {

			String forum_id = forum.getString("forum_id");
			Db.updateMany("Forums", Filters.and(Filters.eq("url", url), Filters.eq("lng", lng)), new Json("$pull", new Json("url", url)));
			Db.updateOne("Forums", Filters.eq("_id", forum_id), new Json("$set", forum.remove("url")).put("$push", new Json("url", new Json("$each", Arrays.asList(url)).put("$position", 0))));

		} else {

			forum.put("lng", lng);
			forum.put("url", Arrays.asList(url));
			Db.save("Forums", forum);
		}
		return forum;
	}

	public static Json editForum(String forum_id) {
		if (forum_id == null) {
			return new Json("error", "NOT_AUTHORIZED");
		}

		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.match(Filters.eq("_id", forum_id)));

		pipeline.add(Aggregates.limit(1));

		pipeline.add(Aggregates.unwind("$parents", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos")));
		pipeline.add(Aggregates.lookup("Forums", "parents", "_id", "parents"));
		pipeline.add(Aggregates.sort(Sorts.ascending("pos")));
		pipeline.add(Aggregates.group("$_id", Arrays.asList(
				Accumulators.first("title", "$title"),
				Accumulators.first("meta_title", "$meta_title"),
				Accumulators.first("url", "$url"),
				Accumulators.first("text", "$text"),
				Accumulators.push("parents", new Json("$arrayElemAt", Arrays.asList("$parents", 0)))
		)));

		pipeline.add(Aggregates.project(new Json("_id", false).put("id", "$_id").put("title", "$title").put("meta_title", "$meta_title").put("url", new Json("$arrayElemAt", Arrays.asList("$url", 0))).put("text", "$text").put("parents", new Json("_id", true).put("title", true))));

		return Db.aggregate("Forums", pipeline).first();

	}

	public static Json search(Json data) {

		String lng = data.getString("lng", Settings.getLang(data.getString("domain", null)));

		Paginer paginer = new Paginer(data.getString("paging"), "title", 30);
		Bson next = paginer.getFilters();

		List<Bson> pipeline = new ArrayList<>();
		List<Bson> filters = new ArrayList<>();
		filters.add(Filters.eq("lng", lng));
		if (!data.getString("search", "").equals("")) {
			filters.add(
					Filters.or(
							Filters.eq("_id", data.getString("search")),
							Filters.regex("title", Pattern.compile(data.getString("search"), Pattern.CASE_INSENSITIVE))
					)
			);
		}

		if (next != null) {
			filters.add(next);
		}
		if (data.containsKey("filter") && data.get("filter") != null) {
			filters.add(Filters.nin("_id", data.getList("filter")));
		}

		if (filters.size() > 0) {
			pipeline.add(Aggregates.match(Filters.and(filters)));
		}
		pipeline.add(paginer.getFirstSort());
		pipeline.add(paginer.getLimit());
		pipeline.add(Aggregates.project(new Json("_id", false).put("id", "$_id").put("title", "$title").put("date", true)));

		pipeline.add(paginer.getLastSort());
		return paginer.getResult("Forums", pipeline);
	}

	public static Json getChildrens(String parent) {

		Json rez = new Json();
		if (parent == null || (parent != null && parent.equals(""))) {
			return null;
		}
		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.match(Filters.eq("_id", parent)));

		pipeline.add(Aggregates.lookup("Forums", "_id", "parents", "childrens"));
		pipeline.add(Aggregates.unwind("$childrens"));

		pipeline.add(Aggregates.project(new Json().put("_id", false).put("id", "$_id").put("order", true).put("childrens",
				new Json().put("id", "$childrens._id").put("title", true).put("order", new Json("$indexOfArray", Arrays.asList("$order", "$childrens._id")))
		)));
		pipeline.add(Aggregates.replaceRoot("$childrens"));
		pipeline.add(Aggregates.lookup("Forums", "id", "parents", "childrens"));
		pipeline.add(Aggregates.sort(Sorts.ascending("order")));
		pipeline.add(Aggregates.project(new Json().put("id", "$id").put("title", "$title").put("childrens", new Json("$size", "$childrens"))));

		List<Json> forums = Db.aggregate("Forums", pipeline).into(new ArrayList<>());
		rez.put("result", forums);
		return rez;
	}


	public static Json orderForum(Json data) {
		List<Json> arbo = data.getListJson("arbo");
		if (arbo == null) {
			return new Json("ok", false);
		}
		for (Json item : arbo) {
			if (item.getList("parents") != null && item.getList("parents").size() > 0) {
				Db.updateOne("Forums", Filters.eq("_id", item.getId()), new Json("$set", new Json("parents", item.getList("parents")).put("order", item.getList("order"))));
			}
		}
		Db.updateMany("Forums",
				Filters.and(Filters.eq("parents", null), Filters.not(Filters.size("parents", 0))),
				new Json("$set", new Json("parents", Arrays.asList())));
		return new Json("ok", true);
	}

	public static Json addChildren(String id, String forum_id) {

		if (Db.updateOne("Forums", Filters.and(Filters.eq("_id", id), Filters.ne("parents", forum_id)),
				Updates.push("parents", forum_id)).getModifiedCount() > 0) {
			return new Json("ok", true);
		}
		return new Json("ok", false);
	}

	public static Json sortChildrens(String forum_id, List<String> order) {

		Db.updateOne("Forums", Filters.eq("_id", forum_id), Updates.set("order", order));
		return new Json("ok", true);
	}

	public static Json addParent(String forum_id, String parent_id) {

		Db.updateOne("Forums", Filters.and(Filters.eq("_id", forum_id), Filters.eq("parents", null)), new Json("$set", new Json("parents", Arrays.asList())));
		if (Db.updateOne("Forums", Filters.and(Filters.eq("_id", forum_id), Filters.ne("parents", parent_id)), Updates.push("parents", parent_id)).getModifiedCount() > 0) {
			return new Json("ok", true);
		}
		return new Json("ok", false);
	}

	public static Json removeParent(String forum_id, String parent_id) {
		return new Json("ok", Db.updateOne("Forums", Filters.and(Filters.eq("_id", forum_id), Filters.eq("parents", parent_id)), Updates.pull("parents", parent_id)).getModifiedCount() > 0);
	}

	public static Json sortParents(String forum_id, List<String> parents) {

		if (Db.updateOne("Forums", Filters.eq("_id", forum_id), Updates.set("parents", parents)).getMatchedCount() > 0) {
			return new Json("ok", true);
		}
		return new Json("ok", false);
	}

	public static Json sortPages(String forum_id, List<String> pages) {
		Db.updateOne("Forums", Filters.eq("_id", forum_id), Updates.set("pages", pages));
		return new Json("ok", true);
	}

	public static boolean racineError(String forum_id) {

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.match(Filters.eq("_id", forum_id)));
		pipeline.add(Aggregates.graphLookup("Forums", "$parents", "parents", "_id", "breadcrumb", new GraphLookupOptions().depthField("depth").maxDepth(1000)));
		//pipeline.add(Aggregates.graphLookup("Pages", new live.page.web.utils.json.Json("$arrayElemAt", Arrays.asList("$parents", 0)), "parents.0", "_id", "breadcrumb", new GraphLookupOptions().depthField("depth").maxDepth(1000)));

		pipeline.add(Aggregates.unwind("$breadcrumb", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.match(Filters.eq("breadcrumb.parents", new ArrayList<>())));
		return Db.aggregate("Forums", pipeline).into(new ArrayList<>()).size() == 0;
	}

	public static Json sortRoot(List<String> orders) {
		int position = 0;
		for (String _id : orders) {
			Db.updateOne("Forums", Filters.eq("_id", _id), new Json("$set", new Json("position", position)));
			position++;
		}
		return new Json("ok", true);
	}
}

