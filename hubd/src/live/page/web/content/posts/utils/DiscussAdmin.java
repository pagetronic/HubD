/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.posts.utils;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import live.page.web.content.congrate.CoinsUtils;
import live.page.web.system.db.Db;
import live.page.web.system.db.tags.DbTags;
import live.page.web.system.json.Json;
import live.page.web.system.sessions.Users;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DiscussAdmin {
	public static Json split(Json data, Users user) {

		String title = data.getString("title");
		String parent = data.getString("parent", "ROOT");
		if (parent.equals("")) {
			parent = "ROOT";
		}
		List<String> posts_ids = data.getList("posts");
		DbTags parentParse = new DbTags(data.getString("parent"));
		Json first = Db.find("Posts", Filters.in("_id", posts_ids)).sort(Sorts.ascending("date")).first();
		Json last = Db.find("Posts", Filters.in("_id", posts_ids)).sort(Sorts.descending("date")).first();

		Json question = new Json();
		question.put("sysid", first.getString("sysId"));
		question.put("user", first.get("user"));
		question.put("title", title);

		question.put("parents", Arrays.asList("Forums(" + parent + ")"));
		question.put("date", first.getDate("date"));
		question.put("last", last.getDate("date"));
		question.put("update", new Date());
		question.put("replies", posts_ids.size() - 1);
		Db.save("Posts", question);

		Db.updateMany("Posts", Filters.in("_id", posts_ids),
				new Json("$set", new Json("parent", question.getId()))
		);
		Db.updateOne(parentParse.getCollection(), Filters.eq("_id", parentParse.getId()),
				new Json("$inc", new Json("replies", -posts_ids.size()))
		);
		Db.updateOne("Posts", Filters.eq("_id", first.getId()),
				new Json("$set", new Json("title", title))
		);
		return ThreadsAggregator.getThread(question.getId(), user, null, false);
	}

	public static Json relocatePostThread(String _id, String to, Users user) {
		Json rez = new Json();

		Json last = Db.find("Posts", Filters.eq("parent", _id)).sort(Sorts.descending("date")).first();
		Json first = Db.find("Posts", Filters.eq("parent", _id)).sort(Sorts.ascending("date")).first();
		long replies = Db.count("Posts", Filters.eq("parent", _id));
		Db.updateMany("Posts", Filters.eq("parent", _id), new Json("$set", new Json("parent", to).put("title", null)));

		Json question = ThreadsAggregator.getThread(to, user, null, false);

		Db.updateMany("Posts", Filters.eq("_id", to), new Json()
				.put("$set",
						new Json("update", new Date()).put("last", last.getDate("date"))
				)
				.put("$inc", new Json("replies", replies))
		);

		Db.deleteOne("Posts", Filters.eq("_id", _id));

		Db.updateMany("Follows", Filters.eq("obj", "Posts(" + _id + ")"), new Json("$set", new Json("obj", "Posts(" + to + ")")));

		rez.put("url", question.getString("url") + "#" + first.getId());
		return rez;
	}


	public static Json move(String id, List<String> parents, Users user) {

		return new Json("ok", Db.updateOne("Posts", Filters.eq("_id", id), new Json("$set", new Json("parents", parents))).getMatchedCount() > 0);
	}

	public static Json addParent(String id, String parent_id, Users user) {

		return new Json("ok", Db.updateOne("Posts", Filters.and(Filters.eq("_id", id), Filters.ne("parents", parent_id)), Updates.push("parents", parent_id)).getModifiedCount() > 0);
	}

	public static Json removeParent(String id, String parent_id, Users user) {

		if (Db.updateOne("Posts", Filters.and(Filters.eq("_id", id), Filters.eq("parents", parent_id), Filters.not(Filters.size("parents", 1))), Updates.pull("parents", parent_id)).getModifiedCount() > 0) {
			return new Json("ok", true);
		}
		return new Json("error", "ONE_PARENT_NEEDED");

	}

	public static Json sortParents(String id, List<String> forums, Users user) {

		Json thread = Db.findById("Posts", id);
		List<String> parents = thread.getList("parents");
		if (parents == null) {
			parents = new ArrayList<>();
		}
		for (String forum : forums) {
			forum = "Forums(" + forum + ")";
			parents.remove(forum);
			parents.add(forum);
		}
		if (Db.updateOne("Posts", Filters.eq("_id", id), Updates.set("parents", parents)).getMatchedCount() > 0) {
			return new Json("ok", true);
		}
		return new Json("ok", false);
	}

	public static Json addPages(String id, String page_id, Users user) {
		if (Db.updateOne("Posts", Filters.and(Filters.eq("_id", id), Filters.ne("parents", "Pages(" + page_id + ")")), Updates.push("parents", "Pages(" + page_id + ")")).getModifiedCount() > 0) {
			return new Json("ok", true);
		}
		return new Json("ok", false);
	}

	public static Json removePages(String id, String page_id, Users user) {

		if (Db.updateOne("Posts", Filters.eq("_id", id), Updates.pull("parents", "Pages(" + page_id + ")")).getModifiedCount() > 0) {
			return new Json("ok", true);
		}
		return new Json("error", "ONE_PARENT_NEEDED");

	}

	public static Json sortPages(String id, List<String> pages, Users user) {

		Json thread = Db.findById("Posts", id);
		List<String> parents = thread.getList("parents");
		if (parents == null) {
			parents = new ArrayList<>();
		}
		for (String page : pages) {
			page = "Pages(" + page + ")";
			parents.remove(page);
			parents.add(page);
		}
		if (Db.updateOne("Posts", Filters.eq("_id", id), Updates.set("parents", parents)).getMatchedCount() > 0) {
			return new Json("ok", true);
		}
		return new Json("ok", false);
	}

	public static void updateSysId(String sysid, String user_id) {
		if (sysid != null && user_id != null) {
			long num_questions = Db.updateMany("Posts", Filters.eq("sysid", sysid),
					new Json("$set", new Json("user", user_id)).put("$unset", new Json("sysid", ""))
			).getModifiedCount();

			CoinsUtils.coinsUser(((int) num_questions) * CoinsUtils.THREAD, user_id);
		}
	}
}
