/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin.utils.scrap;

import com.mongodb.client.model.*;
import live.page.web.admin.utils.Scrapper;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.json.Json;
import live.page.web.system.socket.SessionData;
import live.page.web.system.socket.SocketMessage;
import live.page.web.utils.scrap.ScrapDataUtils;
import live.page.web.utils.scrap.ScrapLinksUtils;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;


public class ScrapAdminUtils {

	public static Json getScraps(String lng, String sort, String paging_str) {

		Paginer paginer = new Paginer(paging_str, sort, 10);
		List<Bson> pipeline = new ArrayList<>();
		List<Bson> filters = new ArrayList<>();
		Aggregator grouper = new Aggregator("id", "title", "date", "last", "url", "cleaner", "scraps", "aggregater", "link", "exclude");

		filters.add(Filters.eq("lng", lng));

		Bson paging = paginer.getFilters();
		if (paging != null) {
			filters.add(paging);
		}

		pipeline.add(Aggregates.match(Filters.and(filters)));

		pipeline.add(paginer.getFirstSort());

		pipeline.add(paginer.getLimit());

		pipeline.add(Aggregates.unwind("$scraps", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos")));
		pipeline.add(Aggregates.unwind("$scraps.forums", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_forum")));

		pipeline.add(Aggregates.lookup("Forums", "scraps.forums", "_id", "scraps_forums"));

		pipeline.add(Aggregates.unwind("$scraps_forums", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.sort(Sorts.ascending("pos_forum")));

		pipeline.add(Aggregates.group(new Json("_id", "$_id").put("pos", "$pos"), grouper.getGrouper(
				Accumulators.push("scraps_forums", "$scraps_forums.title")
		)));

		pipeline.add(Aggregates.sort(Sorts.ascending("_id.pos")));
		pipeline.add(Aggregates.project(grouper.getProjection().put("scraps",
				new Json().put("forums", "$scraps_forums").put("zone", "$scraps.zone").put("include", "$scraps.include")
		)));

		pipeline.add(Aggregates.group("$_id._id", grouper.getGrouper(
				Accumulators.push("scraps", "$scraps")
		)));

		pipeline.add(paginer.getLastSort());

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		return paginer.getResult("Scraps", pipeline);
	}

	public static Json save(String id, String url, String cleaner, String lng, List<Json> scraps, boolean aggregater, String link, String exclude) {
		Json scrap = id != null ? Db.findById("Scraps", id) : new Json();
		if (!scrap.containsKey("title")) {
			scrap.put("title", ScrapDataUtils.getTitle(url));
		}
		Date date = new Date();
		scrap.put("lng", lng).put("url", url).put("cleaner", cleaner).put("date", date)
				.put("scraps", scraps).put("aggregater", aggregater)
				.put("link", link).put("exclude", exclude);

		return new Json("ok", Db.save("Scraps", scrap));
	}

	public static Json edit(String id) {
		return Db.findById("Scraps", id);
	}

	public static boolean refresh(String id) {
		Scrapper.scrapAndPost(Db.findById("Scraps", id));
		return true;
	}

	public static Json delete(String id) {
		return new Json("ok", Db.deleteOne("Scraps", Filters.eq("_id", id)));
	}

	public static SocketMessage scrapPreview(Json msg, SessionData session) {
		Json data = msg.getJson("data");
		String act = msg.getString("act");
		SocketMessage socketMsg = new SocketMessage(act).putKeyMessage("ok", true);
		try {
			return socketMsg;
		} finally {
			Executors.newSingleThreadExecutor().submit(() -> {
				try {
					ScrapLinksUtils.scrap((scrap) -> {
								socketMsg.setMessage(scrap);
								session.send(socketMsg);
								return session.isOpen() && !session.isAbort(act);
							}, data.getString("url"), data.getString("cleaner"), data.getString("lng"), data.getListJson("scraps"),
							data.getBoolean("aggregater", false), data.getString("link"), data.getString("exclude"), true);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		}
	}
}

