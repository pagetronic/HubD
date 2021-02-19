/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin.utils;

import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import live.page.web.blobs.BlobsUtils;
import live.page.web.content.notices.Notifier;
import live.page.web.content.posts.utils.ThreadsAggregator;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;
import live.page.web.utils.scrap.ScrapLinksUtils;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Scrapper {

	public static void scrapAndPost(Json scrapper) {
		try {
			ScrapLinksUtils.scrap(
					(data) -> {
						if (data.getList("forums") != null && scrapper != null && data.containsKey("url")) {
							postAutoLink(data, data.getList("forums"));
							Db.updateOne("Scraps", Filters.eq("_id", scrapper.getId()), new Json("$set", new Json("last", new Date())));
							Fx.log("Scrap found " + data.getString("title") + " @ " + data.getString("url"));
						}
						return true;
					},
					scrapper.getString("url"),
					scrapper.getString("cleaner"),
					scrapper.getString("lng"),
					scrapper.getListJson("scraps"),
					scrapper.getBoolean("aggregater", false),
					scrapper.getString("link"),
					scrapper.getString("exclude"), false
			);
		} catch (InterruptedException ignored) {
		}
	}

	public static void postAutoLink(Json data, List<String> forums) {
		try {

			if (!Db.exists("Forums", Filters.in("_id", forums))) {
				forums = Arrays.asList("ROOT");
			}
			List<String> parents = new ArrayList<>();
			List<Bson> parentsFilter = new ArrayList<>();
			for (String forum : forums) {
				parents.add("Forums(" + forum + ")");
				parentsFilter.add(Filters.eq("parents", "Forums(" + forum + ")"));
			}
			Json exist = Db.find("Posts",
					Filters.and(
							Filters.eq("title", data.getString("title", "")),
							Filters.or(parentsFilter)
					)
			).first();

			Json thread = new Json();
			thread.put("docs", new ArrayList<>());
			thread.put("comments", new ArrayList<>());

			Json link = new Json()
					.put("title", data.getString("title"))
					.put("url", data.getString("url"))
					.put("description", data.getText("description"));
			if (data.containsKey("video")) {
				link.put("video", data.getString("video"));
			}
			List<String> logos = data.getList("logos");
			if (logos != null) {
				for (String logo : logos) {
					String image = null;
					try {
						image = BlobsUtils.downloadToDb(logo, 500);
						link.put("image", image);
					} catch (Exception ignore) {
					}
					if (image != null) {
						break;
					}
				}

			}
			thread.put("link", link);
			thread.put("ip", "127.0.0.1");
			thread.put("title", data.getString("title"));
			thread.put("parents", parents);
			Date date = new Date();
			thread.put("date", date);
			if (!data.getText("text", "").equals("")) {
				thread.put("text", data.getText("text", ""));
			}
			thread.put("last", new Json("date", date));
			thread.put("update", date);
			thread.put("replies", 0);
			String lng = "fr";
			try {
				lng = Db.find("Forums", Filters.eq("_id", forums.get(0))).first().getString("lng");
			} catch (Exception ignore) {
			}

			thread.put("lng", lng);
			thread.put("index", true);
			if (exist != null) {
				UpdateResult rez = Db.updateOne("Posts", Filters.eq("_id", exist.getId()), new Json("$set", thread.remove("date")));
				Fx.log("Modified " + rez.getModifiedCount() + "/" + thread.getId());
			} else {
				Db.save("Posts", thread);
				thread = ThreadsAggregator.getSimplePost(thread.getId());
				String title = link.getString("title");
				String message = link.getText("description");
				String url = Settings.HTTP_PROTO + thread.getString("domain") + thread.getString("url");
				Notifier.notify(thread.getList("roots"), Arrays.asList(), title, message, url, lng);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

