/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.posts.utils;

import com.mongodb.client.model.*;
import live.page.web.content.congrate.CoinsUtils;
import live.page.web.content.notices.Notifier;
import live.page.web.content.users.UsersAggregator;
import live.page.web.system.Settings;
import live.page.web.system.cosmetic.tmpl.BaseTemplate;
import live.page.web.system.db.Db;
import live.page.web.system.db.tags.DbTags;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.utils.Antiflood;
import live.page.web.system.sessions.Users;
import live.page.web.system.socket.SocketPusher;
import live.page.web.utils.Fx;
import org.bson.conversions.Bson;

import java.util.*;

public class DiscussPoster {

	public static Json post(Json data, Users user, String ip) {

		for (String key : Arrays.asList("title", "text", "link_title", "link_description")) {
			if (data.get(key) != null) {
				data.put(key, Fx.normalizePost(data.getText(key)));
			}
		}

		if (!Settings.getLangs().contains(data.getString("lng", Settings.getLang(data.getString("domain"))))) {
			return new Json("error", "NO_LANG");
		}

		Json ctrl = control(data, user, ip);

		if (ctrl.containsKey("errors") || ctrl.containsKey("error")) {
			return ctrl;
		}

		if (data.getString("title", "").equals("") && data.containsKey("page")) {
			data.put("title", Fx.truncate(data.getString("text"), 60));
		}

		if (data.getId() != null) {
			return postEdit(data, user, ip);
		}

		List<DbTags> parents = data.getParents("parents");
		if (parents == null) {
			parents = Arrays.asList(data.getParent("parent"));
		}
		return postPost(parents, data, user, ip);
	}


	private static Json control(Json data, Users user, String ip) {
		Json response = new Json();

		if (Antiflood.isFlood(user != null ? user.getId() : ip)) {
			return new Json("error", "FLOOD_ERROR").put("delay", Settings.FLOOD_DELAY / 1000);
		}

		if (data.getString("link_title", "").equals("") && data.getString("link_url", "").equals("")
				&& (!data.containsKey("text") || data.getText("text").length() < (data.containsKey("page") ? 10 : 3))) {
			response.add("errors", new Json("element", "text").put("message", "TEXT_TOO_SHORT"));
		} else if (data.containsKey("text") && data.getText("text").length() > 20000) {
			response.add("errors", new Json("element", "text").put("message", "TEXT_TOO_LONG"));
		}

		if (!data.getString("link_title", "").equals("") && !data.getString("link_url", "").equals("")) {

			if (data.getText("link_title", "").length() < 5) {
				response.add("errors", new Json("element", "link_title").put("message", "TITLE_TOO_SHORT"));
			} else if (data.getText("link_title").length() > 100) {
				response.add("errors", new Json("element", "link_title").put("message", "TITLE_TOO_LONG"));
			}

			if (data.getText("link_description", "").length() < 5) {
				response.add("errors", new Json("element", "link_description").put("message", "TEXT_TOO_SHORT"));
			} else if (data.getText("link_description").length() > 3000) {
				response.add("errors", new Json("element", "link_description").put("message", "TEXT_TOO_LONG"));
			}
		}

		if (data.containsKey("title")) {
			if (data.getString("title").length() > 100) {
				response.add("errors", new Json("element", "title").put("message", "TITLE_TOO_LONG"));
			} else if (data.getString("title").length() < 10) {
				response.add("errors", new Json("element", "title").put("message", "TITLE_TOO_SHORT"));
			}
		}

		if (data.containsKey("docs")) {
			for (String doc : data.getList("docs")) {
				if (!Db.exists("BlobFiles", Filters.eq("_id", doc))) {
					response.add("errors", new Json("element", "docs").put("message", "INVALID_DOC"));
				}
			}
		}

		return response;
	}

	private static Json postPost(List<DbTags> parents, Json data, Users user, String ip) {
		String lng = data.getString("lng", Settings.getLang(data.getString("domain")));
		Date date = new Date();
		Json response = new Json();

		Json post = new Json();

		if (user == null) {
			if (!data.getString("sysId", "").equals("")) {
				post.put("sysid", data.getString("sysId"));
			}
		} else {
			post.put("user", user.getId());
		}

		post.put("date", date);

		if (data.getString("title") != null) {
			post.put("title", data.getString("title"));
		}
		post.put("text", data.getText("text"));
		post.put("docs", data.getList("docs") == null ? new ArrayList<>() : data.getList("docs"));

		if (!data.getString("link_title", "").equals("") && !data.getString("link_url", "").equals("")) {

			Json link = new Json().put("title", data.getString("link_title")).put("url", data.getString("link_url")).put("description", data.getText("link_description"));
			if (!data.getString("link_image", "").equals("")) {
				link.put("image", data.getString("link_image"));
			}
			post.put("link", link);
		}

		post.put("ip", ip);


		if (user == null) {
			if (!data.getString("sysId", "").equals("")) {
				post.put("sysid", data.getString("sysId"));
			}
		} else {
			post.put("user", user.getId());
		}


		boolean isReply = true;
		for (DbTags parent : parents) {
			post.add("parents", parent.toString());
			if (parent.getCollection().equals("Forums")) {
				isReply = false;
			}
		}

		if (!isReply) {
			post.put("title", data.getString("title", Fx.truncate(data.getString("text"), 70)));
		}
		post.put("date", date).put("last", new Json("date", date)).put("update", date).put("replies", 0);

		post.put("lng", lng);

		boolean concat = false;
		Json previous = null;
		if (isReply) {
			previous = Db.find("Posts", Filters.or(
					Filters.eq("_id", parents.get(0).getId()),
					Filters.and(Filters.eq("parents", post.getList("parents")), Filters.exists("remove", false))
					)
			).sort(Sorts.descending("date")).first();

			if (!Fx.IS_DEBUG && previous != null &&
					previous.getString("user") != null &&
					previous.getString("user").equals(user.getId())) {

				Calendar delay = Calendar.getInstance();
				delay.add(Calendar.HOUR, -4);
				Date last_date = previous.getDate("update", previous.getDate("date", date));
				if (last_date.after(delay.getTime())) {
					concat = true;
				}

			}
		}

		if (concat) {

			Json update = new Json("$set", new Json("text", previous.getText("text") + "\n\n" + data.getText("text")).put("ip", ip).put("update", date));

			if (data.getList("docs") != null) {
				update.put("$addToSet", new Json("docs", new Json("$each", data.getList("docs"))));
			}
			post = Db.findOneAndUpdate("Posts", Filters.eq("_id", previous.getId()), update, new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

			for (DbTags parent : parents) {
				Db.updateOne(parent.getCollection(), Filters.eq("_id", parent.getId()), new Json().put("$set", new Json("update", date)));
			}

		} else {

			if (Db.save("Posts", post)) {

				if (user != null) {
					CoinsUtils.coinsUser(isReply ? CoinsUtils.POST : CoinsUtils.THREAD, user.getId());
				}


				for (DbTags parent : parents) {

					Db.updateOne(parent.getCollection(), Filters.eq("_id", parent.getId()),
							new Json()
									.put("$set",
											new Json("last", new Json("user", post.getString("user")).put("date", post.getDate("date")).put("id", post.getId())).put("update", date)
									)
									.put("$inc", new Json("replies", 1))
					);
				}
			} else {
				return new Json("error", "INVALID_DATA");
			}

		}


		post = ThreadsAggregator.getSimplePost(post.getId());

		response.put(isReply ? "post" : "thread", post);
		response.put("url", post.getString("url"));

		if (parents.size() > 0) {
			Json push_infos = new Json("id", post.getId()).put("title", post.getString("title")).put("date", post.getDate("date"))
					.put("url", post.getString("url")).put("user", post.getJson("user"));

			List<String> parents_clean = new ArrayList<>();
			for (DbTags parent : parents) {
				SocketPusher.send(parent.getCollection().toLowerCase() + "/" + parent.getId(), push_infos);
				parents_clean.add(parent.toString());

			}

			List<String> excludes = new ArrayList<>();
			if (user != null) {
				excludes.add(user.getId());
			}
			String title = post.getString("title");
			String message = post.getText("text", "");
			if (message.equals("") && post.get("link") != null) {
				Json link = post.getJson("link");
				message += link.getString("title") + ": " + post.getText("description");
			}

			String url = Settings.HTTP_PROTO + post.getString("domain") + post.getString("url");
			Notifier.notify(post.getList("roots"), excludes, title, message, url, lng);

		}
		response.put("html", BaseTemplate.processToString(isReply ? "/threads/post_item.html" : "/threads/thread_item.html", response.clone().put("tz", data.getInteger("tz", 0)).put("user", user).put("lng", data.getString("lng"))));
		response.put("ok", true);
		return response;
	}

	private static Json postEdit(Json data, Users user, String ip) {

		if (user == null) {
			return new Json("error", "PLEASE_LOGIN");
		}

		Date date = new Date();
		List<Bson> filter = new ArrayList<>();
		filter.add(Filters.eq("_id", data.getId()));
		if (!user.getEditor()) {
			filter.add(Filters.eq("user", user.getId()));
		}

		Json previous = Db.find("Posts", Filters.and(filter)).first();
		if (previous == null) {
			return new Json("error", "NOT_FOUND");
		}
		Json update = new Json();

		Json set = new Json();
		Json change = new Json();
		Json thread = new Json("id", data.getId() != null ? data.getId() : previous.getString("thread"));

		if (data.containsKey("title") && !data.getString("title").equals(previous.getString("title"))) {
			change.put("title", previous.getString("title"));
			set.put("title", data.getString("title"));
			thread.put("title", data.getString("title"));
		}
		if (data.containsKey("text") && !data.getText("text").equals(previous.getText("text"))) {
			change.put("text", previous.getText("text"));
			set.put("text", data.getText("text"));
		}
		if (data.containsKey("docs") && !data.getList("docs").equals(previous.getList("docs"))) {
			change.put("docs", previous.getList("docs"));
			set.put("docs", data.getList("docs"));
		}


		if (!data.getString("link_title", "").equals("") && !data.getString("link_url", "").equals("")) {
			Json link_previous = previous.getJson("link");
			if (link_previous == null) {
				link_previous = new Json();
			}
			Json link_new = new Json()
					.put("title", Fx.normalizePost(data.getString("link_title")))
					.put("url", data.getString("link_url"))
					.put("description", Fx.normalizePost(data.getText("link_description")));

			if (!data.getString("link_image", "").equals("")) {
				link_new.put("image", data.getString("link_image"));
			}

			if (!link_previous.toString(true).equals(link_new.toString(true))) {

				change.put("link", link_previous);
				set.put("link", link_new);
			}
		}

		if (data.getBoolean("link_remove", false)) {
			update.put("$unset", new Json("link", ""));
			change.put("link", previous.getJson("link"));
		}


		if (change.isEmpty()) {
			return new Json("ok", "NO_MODIFICATIONS");
		}


		change.put("ip", ip);

		set.put("update", date);

		change.put("user", user.getId()).put("date", date);

		update.put("$set", set);
		update.put("$push", new Json("changes", new Json("$each", Arrays.asList(change)).put("$position", 0)));

		Json new_post = Db.findOneAndUpdate("Posts", Filters.and(filter), update, new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

		if (new_post == null) {
			return new Json("error", "NOT_FOUND");
		}

		Json update_thread = new Json("update", date);
		if (thread.containsKey("title")) {
			update_thread.put("title", thread.getString("title"));
		}
		Db.updateOne("Posts", Filters.eq("_id", previous.getString("thread")), new Json("$set", update_thread));

		return new Json("ok", true).put("post", ThreadsAggregator.getSimplePost(data.getId()));
	}


	public static Json comment(Json data, Users user, String lng, String ip) {
		if (user == null) {
			return new Json("error", "PLEASE_LOGIN");
		}
		if (data.get("text") != null) {
			data.put("text", Fx.normalizePost(data.getText("text")));
		}

		Json response = new Json();

		try {
			if (data.get("text") == null || data.getText("text").length() < 3) {
				response.add("errors", new Json("element", "text").put("message", "TEXT_TOO_SHORT"));
			} else if (data.getText("text").length() > 185) {
				response.add("errors", new Json("element", "text").put("message", "TEXT_TOO_LONG"));
			} else if (Antiflood.isFlood(user.getId())) {
				return new Json("error", "FLOOD_ERROR").put("delay", Settings.FLOOD_DELAY / 1000);
			}
			if (!response.containsKey("errors") && !response.containsKey("error")) {

				Json comment = new Json();
				comment.put("user", user.getId());
				comment.put("text", data.getText("text"));
				comment.put("date", new Date());
				comment.put("ip", ip);

				Json post = Db.findOneAndUpdate("Posts",
						Filters.eq("_id", data.getString("post_id")),
						new Json("$push", new Json("comments", comment))
				);

				if (post == null) {
					return new Json("error", "POST_NOT_EXIST");
				}

				comment.put("user", UsersAggregator.simpleUser(user));

				comment.put("index", post.getListJson("comments").size() - 1).remove("ip");
				comment.put("id", post.getId() + "_" + comment.getInteger("index"));
				response.put("comment", comment);
				response.put("post", new Json("id", data.getString("post_id")));
				SocketPusher.send("posts/" + post.getString("thread"), new Json("comments", comment));


				for (DbTags parent : post.getParents("parents")) {
					if (parent.getCollection().equals("Posts")) {
						Json thread = ThreadsAggregator.getSimplePost(parent.getId());
						if (thread != null) {
							String url = Settings.HTTP_PROTO + thread.getString("domain") + thread.getString("url") + "?post=" + post.getId() + "#" + comment.getId();
							Notifier.notify(Arrays.asList(parent.toString()), Arrays.asList(user.getId()), thread.getString("title"), comment.getString("text"), url, lng);
							break;
						}
					}
				}


			}
			if (!response.containsKey("errors") && !response.containsKey("error")) {

				response.put("html", BaseTemplate.processToString("/threads/post_item_tips.html", response.clone().put("tz", data.getInteger("tz", 0)).put("user", user).put("lng", lng)));
			}

			return response;
		} catch (Exception e) {
			if (Fx.IS_DEBUG) {
				e.printStackTrace();
			}
			response.add("errors", "CRASH");
			return response;
		}
	}

	public static Json remove(Json data, Users user) {


		if (user == null) {
			return new Json("error", "PLEASE_LOGIN");
		}

		Date date = new Date();

		Json change = new Json("user", user.getId()).put("date", date);

		if (data.containsKey("comment")) {

			List<Bson> filter = new ArrayList<>();
			filter.add(Filters.eq("_id", data.getId()));
			if (!user.getEditor()) {
				filter.add(Filters.eq("comments." + data.getInteger("comment") + ".user", user.getId()));
			}

			Json update = new Json();
			if (data.getBoolean("restore", false)) {
				change.put("restore", date);
				update.put("$unset", new Json("comments." + data.getInteger("comment") + ".remove", ""));
			} else {
				change.put("remove", date);
				update.put("$set", new Json("comments." + data.getInteger("comment") + ".remove", date));
			}

			update.put("$push", new Json("comments." + data.getInteger("comment") + ".changes", new Json("$each", Arrays.asList(change)).put("$position", 0)));


			Json post = Db.findOneAndUpdate("Posts", Filters.and(filter), update, new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE));
			if (post == null) {
				return new Json("ok", false);
			}

			return new Json("ok", true);

		}

		if (data.getId() != null) {
			Json update = new Json();
			if (data.getBoolean("restore", false)) {
				update.put("$unset", new Json("remove", ""));
				change.put("restore", date);
			} else {
				update.put("$set", new Json("remove", date));
				change.put("remove", date);
			}
			update.put("$push", new Json("changes", new Json("$each", Arrays.asList(change)).put("$position", 0)));

			List<Bson> filter = new ArrayList<>();
			filter.add(Filters.eq("_id", data.getId()));
			if (!user.getEditor()) {
				filter.add(Filters.eq("user", user.getId()));
			}
			Json post = Db.findOneAndUpdate("Posts", Filters.and(filter), update, new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
			if (post != null) {
				for (DbTags parent : post.getParents("parents")) {
					Json update_ = new Json();
					Json last_ = Db.find("Posts", Filters.and(Filters.eq("parents", parent.toString()), Filters.exists("remove", false))).sort(Sorts.descending("date")).first();
					Json last = new Json();
					if (last_ != null) {
						last.put("id", last_.getId()).put("date", last_.getDate("date")).put("user", last_.getString("user"));
					} else {
						last.put("date", Db.findById(parent.getCollection(), parent.getId()).getDate("date"));
					}
					update_.put("$set", new Json("last", last));
					update_.put("$inc", new Json("replies", data.getBoolean("restore", false) ? 1 : -1));
					Db.updateOne(parent.getCollection(), Filters.eq("_id", parent.getId()), update_);
				}
				return new Json("ok", true);
			}
			return new Json("ok", false);

		}


		return new Json("ok", false);

	}


	public static Json history(String post_id, int comment) {

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.match(Filters.and(Filters.eq("_id", post_id))));

		if (comment >= 0) {
			pipeline.add(Aggregates.project(new Json("_id", false).put("comments", true)));
			pipeline.add(Aggregates.project(new Json("changes", new Json("$arrayElemAt", Arrays.asList("$comments", comment)))));
			pipeline.add(Aggregates.project(new Json("changes", "$changes.changes")));
		} else {
			pipeline.add(Aggregates.project(new Json("_id", false).put("changes", true)));
		}

		pipeline.add(Aggregates.unwind("$changes", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("id")));

		pipeline.add(Aggregates.unwind("$changes.docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")));
		pipeline.add(Aggregates.lookup("BlobFiles", "changes.docs", "_id", "changes.docs"));
		pipeline.add(Aggregates.sort(Sorts.ascending("pos_doc")));

		pipeline.add(Aggregates.group("$id", Arrays.asList(
				Accumulators.first("id", "$id"),
				Accumulators.first("user", "$user"),
				Accumulators.first("changes", "$changes"),
				Accumulators.push("changes_docs", new Json("$arrayElemAt", Arrays.asList("$changes.docs", 0)))
		)));

		pipeline.add(Aggregates.lookup("Users", "changes.user", "_id", "changes.user"));
		pipeline.add(Aggregates.unwind("$changes.user", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.sort(Sorts.ascending("id")));
		pipeline.add(Aggregates.project(
				new Json("changes",
						new Json("user", new Json("id", "$changes.user._id").put("name", "$changes.user.name").put("avatar", "$changes.user.avatar"))
								.put("restore", true)
								.put("remove", true)
								.put("title", true)
								.put("text", true)
								.put("date", true)
								.put("docs", "$changes_docs")
				)
		));
		pipeline.add(Aggregates.project(
				new Json("changes",
						new Json("user", true)
								.put("restore", true)
								.put("remove", true)
								.put("title", true)
								.put("text", true)
								.put("date", true)
								.put("docs", new Json("_id", true).put("type", true).put("size", true).put("text", true))
				)
		));

		pipeline.add(Aggregates.group(null, Arrays.asList(
				Accumulators.push("changes", "$changes")
		)));

		try {
			return Db.aggregate("Posts", pipeline).first();
		} catch (Exception e) {
			e.printStackTrace();
			return new Json("error", "UNKNOWN");
		}
	}


	public static Json parents(String id) {
		try {
			return new Json("parents", Db.findById("Posts", id).getList("parents"));
		} catch (Exception e) {
			return new Json("error", "UNKNOWN");
		}
	}
}
