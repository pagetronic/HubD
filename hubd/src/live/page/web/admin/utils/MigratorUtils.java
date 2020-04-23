package live.page.web.admin.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.sessions.Users;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigratorUtils {

	/**
	 * Search Pages
	 *
	 * @param search query
	 * @return Json with result to return to javaScript
	 */
	public static Json searchDestination(String search) {
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

	/**
	 * Migrage Pages to another website
	 *
	 * @param user        who do action
	 * @param ids         of the pages to migrate
	 * @param destination parent where to put the pages in another website
	 * @return Json contains source url fragment and complete destination url
	 */
	public static Json migrate(Users user, List<String> ids, String destination) {
		if (ids == null || destination == null || ids.size() == 0 || destination.equals("")) {
			return new Json("ok", false);
		}
		MongoClient destination_client = Db.getClient(Settings.MIGRATOR_DB_USER, Settings.MIGRATOR_DB_NAME, Settings.MIGRATOR_DB_PASS);
		MongoDatabase destination_base = destination_client.getDatabase(Settings.MIGRATOR_DB_NAME);
		MongoCollection<Json> destination_pages = destination_base.getCollection("Pages", Json.class);

		Json rez = new Json("ok", true);
		for (String id : ids) {
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


				String url = getUrl(destination_pages, page.getId(), Settings.MIGRATOR_LANGS_DOMAINS.getString(page.getString("lng"), ""));

				Db.save("Revisions", new Json("origine", page.getId())
						.put("editor", user.getId()).put("url", original_page.getString("url")).put("301", url).put("edit", new Date()));
				Db.deleteOne("Pages", Filters.eq("_id", original_page.getId()));
				rez.add("redirects", new Json("origin", page.getString("url")).put("destination", url));
			}
		}

		destination_client.close();

		return rez;
	}

	/**
	 * Update remaining DbTags [Pages(XXXIDXXX)], for Pages who's migrated and is now external
	 */
	public static void updateRemainingTags() {

		MongoClient destination_client = Db.getClient(Settings.MIGRATOR_DB_USER, Settings.MIGRATOR_DB_NAME, Settings.MIGRATOR_DB_PASS);
		MongoDatabase destination_base = destination_client.getDatabase(Settings.MIGRATOR_DB_NAME);
		MongoCollection<Json> destination_pages = destination_base.getCollection("Pages", Json.class);
		MongoCollection<Json> destination_revisions = destination_base.getCollection("Revisions", Json.class);

		MongoCursor<Json> pages = Db.find("Pages").iterator();
		while (pages.hasNext()) {
			Json page = pages.next();
			String text = updateMigration(destination_pages, page, Settings.MIGRATOR_LANGS_DOMAINS.getString(page.getString("lng")));
			if (!page.getText("text", "").equals(text)) {
				Db.save("Revisions", new Json("origine", page.getId()).put("text", text).put("edit", new Date()));
				Db.save("Pages", page.put("text", text).put("update", new Date()));
			}
		}
		pages.close();

		MongoCursor<Json> migrator_pages = destination_pages.find().iterator();
		while (migrator_pages.hasNext()) {
			Json page = migrator_pages.next();
			String text = updateMigration(Db.getDb("Pages"), page, Settings.LANGS_DOMAINS.getString(page.getString("lng")));
			if (!page.getText("text", "").equals(text)) {
				destination_revisions.insertOne(new Json().put("_id", Db.getKey()).put("origine", page.getId()).put("text", text).put("edit", new Date()));
				destination_pages.updateOne(Filters.eq("_id", page.getId()), new Json().put("$set", new Json().put("text", text).put("update", new Date())));
			}
		}
		migrator_pages.close();

		destination_client.close();

	}

	/**
	 * Replace Tags and old links in text of a page
	 *
	 * @param destination_pages MongoDb collection that contain Page
	 * @param page              Json that contains the page where the text have to be modified
	 * @param domain            where to link
	 * @return the modified text
	 */
	private static String updateMigration(MongoCollection<Json> destination_pages, Json page, String domain) {
		String text = page.getText("text", "");
		for (String pat : new String[]{"\\[Pages\\(([a-z0-9]+)\\) ?([^]]+)]", "<a original=\"([a-z0-9]+)\"[^>]+>([^<]+)</a>"}) {
			Pattern pattern = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				String title = matcher.group(2);
				String id = matcher.group(1);
				Json migration_page = destination_pages.find(Filters.eq("_id", id)).limit(1).first();
				if (migration_page != null) {
					String url = getUrl(destination_pages, migration_page.getId(), domain);
					String replacement = "<a original=\"" + id + "\" href=\"" + url + "\"";
					if (migration_page.containsKey("top_title")) {
						replacement += " title=\"" + migration_page.getString("top_title").replace("\"", "\\\"") + "\"";
					} else if (!migration_page.getString("title", "").equals(title)) {
						replacement += " title=\"" + migration_page.getString("title").replace("\"", "\\\"") + "\"";
					}
					text = text.replace(matcher.group(), replacement + ">" + title + "</a>");
				}
			}
		}
		return text;
	}

	/**
	 * Link pages in local site to other website with keywords
	 *
	 * @param id       of the local Page to link
	 * @param keywords to link in other website
	 * @return urls of the pages linked
	 */
	public static Json link(String id, List<String> keywords, Users user) {
		Json rez = new Json("ok", true).put("links", new ArrayList<>());
		Json original = Db.findById("Pages", id);
		if (original == null) {
			return new Json("ok", false);
		}
		String url = getUrl(Db.getDb("Pages"), id, Settings.LANGS_DOMAINS.getString(original.getString("lng")));

		MongoClient destination_client = Db.getClient(Settings.MIGRATOR_DB_USER, Settings.MIGRATOR_DB_NAME, Settings.MIGRATOR_DB_PASS);
		MongoDatabase destination_base = destination_client.getDatabase(Settings.MIGRATOR_DB_NAME);
		MongoCollection<Json> destination_pages = destination_base.getCollection("Pages", Json.class);
		MongoCollection<Json> destination_revisions = destination_base.getCollection("Revisions", Json.class);
		MongoCursor<Json> pages = destination_pages.find(Filters.regex("text", Pattern.compile("(" + StringUtils.join(keywords, "|") + ")", Pattern.CASE_INSENSITIVE))).iterator();


		pageloop:
		while (pages.hasNext()) {
			Json page = pages.next();
			String text = page.getText("text", "");
			if (text.contains(id)) {
				continue;
			}

			List<String> groups = new ArrayList<>();
			for (String pat : new String[]{"\\[([^]]+)]", "<a[^>]+>([^<]+)</a>", "=([^\n]+)=([ ]+)?\n"}) {
				Pattern pattern = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(text);
				while (matcher.find()) {
					text = text.replace(matcher.group(), "@@@###" + groups.size() + "###@@@");
					groups.add(matcher.group());
				}
			}

			for (String keyword : keywords) {
				if (keyword.equals("")) {
					continue;
				}
				Pattern pattern = Pattern.compile("([\\r\\n\\t ,’'ʼ(]|^)(" + keyword + ")([.,!?;) ])", Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(text);
				if (matcher.find()) {
					String start = matcher.group(1);
					String title = matcher.group(2);
					String punct = matcher.group(3);

					String replacement = start + "<a original=\"" + id + "\" href=\"" + url + "\"";
					if (original.containsKey("top_title")) {
						replacement += " title=\"" + original.getString("top_title").replace("\"", "\\\"") + "\"";
					} else if (!original.getString("title", "").equals(title)) {
						replacement += " title=\"" + original.getString("title").replace("\"", "\\\"") + "\"";
					}
					text = matcher.replaceFirst(replacement + ">" + title + "</a>" + punct);

					for (int i = groups.size() - 1; i >= 0; i--) {
						text = text.replace("@@@###" + i + "###@@@", groups.get(i));
					}

					destination_revisions.insertOne(new Json().put("_id", Db.getKey()).put("origine", page.getId()).put("editor", user.getId()).put("text", text).put("edit", new Date()));
					destination_pages.updateOne(Filters.eq("_id", page.getId()), new Json().put("$set", new Json().put("text", text).put("update", new Date())));
					rez.add("links", Settings.HTTP_PROTO + Settings.MIGRATOR_LANGS_DOMAINS.getString(page.getString("lng")) + "/" + page.getString("url"));
					continue pageloop;
				}
			}

		}
		pages.close();
		destination_client.close();

		//rez.add("links
		return rez;
	}

	/**
	 * Compose complete url of a Page
	 *
	 * @param destination_pages MongoDb collection that contain Page
	 * @param id                of the Page
	 * @param domain            of the website hosting Page
	 * @return the url
	 */
	static String getUrl(MongoCollection<Json> destination_pages, String id, String domain) {


		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.match(Filters.eq("_id", id)));
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
				.put("url", new Json("$reduce", new Json("input", "$urls").put("initialValue", Settings.HTTP_PROTO + domain).put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this"))))))
		);
		return destination_pages.aggregate(pipeline).first().getString("url");
	}
}
