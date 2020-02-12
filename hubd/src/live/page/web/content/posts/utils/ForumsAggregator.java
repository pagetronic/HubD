/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.posts.utils;

import com.mongodb.client.model.*;
import live.page.web.content.notices.NoticesUtils;
import live.page.web.content.pages.PagesAggregator;
import live.page.web.system.Settings;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.sessions.Users;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ForumsAggregator {

	private static final List<Json> domains = Settings.LANGS_DOMAINS.toList();

	public static Json getForum(String urlOrIdExt, String domainOrLng, String paging_str, Users user, boolean remove) {

		String urlOrId = urlOrIdExt.replaceAll(".*/([^/.]+)(/|\\.json|\\.xml)?$", "$1");
		if (urlOrId == null) {
			return null;
		}
		String lng = Settings.getLang(domainOrLng);
		if (lng == null && Settings.LANGS_DOMAINS.containsKey(domainOrLng)) {
			lng = domainOrLng;
		}
		Json forum = getForum(Filters.and(Filters.eq("lng", lng), Filters.or(Filters.eq("_id", urlOrId), Filters.eq("url", urlOrId))), user, paging_str, remove);


		if (forum == null) {
			return null;
		}
		if (user != null) {
			NoticesUtils.setRead("Forums(" + forum.getId() + ")", user.getId());
		}
		forum.remove("branch");
		return forum;
	}


	public static Json getForum(Bson filter, Users user, String paging_str, boolean remove) {

		Aggregator grouper = new Aggregator("id", "title", "meta_title", "text",
				"url", "domain", "lng", "breadcrumb", "parents", "childrens", "sisters", "menu", "pages", "threads", "order", "sort", "branch"
		);


		List<Bson> pipeline = new ArrayList<>();

		pipeline.addAll(getForumsPipeline(grouper, filter, user != null && user.getEditor(), false));

		pipeline.add(Aggregates.project(new Json("_id", false)
				.put("id", "$_id")
				.put("title", "$title")
				.put("meta_title", "$meta_title")
				.put("text", "$text")
				.put("lng", "$lng")
				.put("domain", "$domain.value")
				.put("url", "$url")
				.put("follow", "$follow")
				.put("breadcrumb", "$breadcrumb")
				.put("parents", "$parents")
				.put("childrens", "$childrens")
				.put("sisters", "$sisters")
				.put("menu", "$menu")
				.put("pages", "$pages")
				.put("branch", "$branch")
		));

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		Json forum = Db.aggregate("Forums", pipeline).first();
		if (forum == null) {
			return null;
		}


		forum.put("threads", ThreadsAggregator.getThreads(Filters.in("parents", forum.getList("branch")), paging_str, false));
		forum.remove("branch");
		return forum;

	}

	public static List<Bson> getForumsPipeline(Aggregator grouper, Bson filter, boolean admin, boolean sortArray) {

		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.match(filter));


		pipeline.add(Aggregates.project(grouper.getProjection().put("sort", sortArray ? new Json("$indexOfArray", Arrays.asList("$$sort", "$_id")) : "$date")));

		pipeline.addAll(getBreadCrumbPipeline(grouper));


		pipeline.addAll(getChildrensPipeline(grouper));

		pipeline.addAll(getSistersPipeline(grouper));

		pipeline.addAll(getParentsPipeline(grouper));

		pipeline.addAll(PagesAggregator.getPagesLookup("_id", "forums", grouper, "pages.forums"));

		pipeline.add(Aggregates.graphLookup("Forums", "$_id", "_id", "parents", "branch", new GraphLookupOptions().maxDepth(1000)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("pages", new Json("id", true)
						.put("title", true)
						.put("top_title", true)
						.put("intro", true)
						.put("logo", true)
						.put("lng", true)
						.put("domain", true)
						.put("url", true)
				)
				.put("branch", new Json("$concatArrays", Arrays.asList("$branch", Arrays.asList(new Json("_id", "$_id")))))
		));

		pipeline.add(Aggregates.unwind("$branch", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("branch", new Json("$concat", Arrays.asList("Forums(", "$branch._id", ")")))
				.put("_id", "$_id")
				.put("breadcrumb", new Json("$filter", new Json("input", "$breadcrumb").put("as", "breadcrumb").put("cond",
						new Json("$ne", Arrays.asList("$$breadcrumb.id", new BsonUndefined()))

				)))
		));
		if (!admin) {
			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("parents", new Json("$filter", new Json("input", "$parents").put("as", "parents").put("cond", new Json("$not", new Json("$in", Arrays.asList("$$parents.id", "$childrens.id"))))))
					.put("sisters", new Json("$filter", new Json("input", "$sisters").put("as", "sisters").put("cond", new Json("$not", new Json("$or", Arrays.asList(new Json("$eq", Arrays.asList("$$sisters.id", "$id")), new Json("$in", Arrays.asList("$$sisters.id", "$parents.id")), new Json("$in", Arrays.asList("$$sisters.id", "$childrens.id"))))))))
			));
		}

		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(
				Accumulators.push("branch", "$branch")
		)));


		pipeline.addAll(getMenuPipeline(grouper));

		pipeline.add(Aggregates.sort(Sorts.ascending("sort")));


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("domain",
						new Json("$arrayElemAt", Arrays.asList(
								new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList("$lng", "$$domains.key"))))
								, 0))
				)
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("breadcrumb", new Json()
						.put("id", true)
						.put("title", true)
						.put("lng", true)
						.put("domain", true)
						.put("url", true)))
		);

		return pipeline;
	}


	private static List<Bson> getChildrensPipeline(Aggregator grouper) {

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.lookup("Forums", "_id", "parents", "childrens"));

		pipeline.add(Aggregates.unwind("$childrens", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.graphLookup("Forums", "$childrens._id", "parents.0", "_id", "childrens.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$childrens.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(new Json("childrens.parents.depth", -1)));

		pipeline.add(Aggregates.group(new Json("childrens_id", "$childrens._id").put("_id", "$_id"),
				grouper.getGrouper(
						Accumulators.first("order_", "$order_"),
						Accumulators.push("urls", new Json("$arrayElemAt", Arrays.asList("$childrens.parents.url", 0)))
				)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("orderc", new Json("$indexOfArray", Arrays.asList("$order_", "$childrens._id")))
				.put("childrens", new Json("_id", true).put("id", true).put("title", true).put("lng", true)
						.put("domain",
								new Json("$arrayElemAt", Arrays.asList(
										new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList("$childrens.lng", "$$domains.key"))))
										, 0))
						)
						.put("url", new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))))

				)
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("orderc", true)
				.put("childrens", new Json("_id", true)
						.put("id", "$childrens._id")
						.put("title", "$childrens.title")
						.put("lng", "$childrens.lng")
						.put("domain", "$childrens.domain.value")
						.put("url", "$childrens.url")

				)
		));
		pipeline.add(Aggregates.sort(Sorts.ascending("orderc")));

		pipeline.add(Aggregates.group("$_id._id",
				grouper.getGrouper(
						Accumulators.push("childrens", "$childrens")
				)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("childrens",
						new Json("$filter", new Json("input", "$childrens").put("as", "childrens").put("cond", new Json("$ne", Arrays.asList("$$childrens._id", new BsonUndefined()))))
				)
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("childrens", new Json("title", true).put("url", true).put("id", true).put("lng", true).put("domain", true))
		));
		return pipeline;
	}

	public static List<Bson> getParentsPipeline(Aggregator grouper) {

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.project(grouper.getProjection().put("parents_order", "$parents")));

		pipeline.add(Aggregates.lookup("Forums", "parents", "_id", "parents"));

		pipeline.add(Aggregates.unwind("$parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.graphLookup("Forums", "$parents._id", "parents.0", "_id", "parents.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$parents.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(new Json("parents.parents.depth", -1)));

		pipeline.add(Aggregates.group(new Json("parents_id", "$parents._id").put("_id", "$_id"),
				grouper.getGrouper(
						Accumulators.first("parents_order", "$parents_order"),
						Accumulators.push("urls", new Json("$arrayElemAt", Arrays.asList("$parents.parents.url", 0)))
				)
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("pos", new Json("$indexOfArray", Arrays.asList("$parents_order", "$parents._id")))
				.put("parents",
						new Json("_id", true).put("date", true).put("title", true).put("lng", true)
								.put("url", new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))))
								.put("domain",
										new Json("$arrayElemAt", Arrays.asList(
												new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList("$parents.lng", "$$domains.key"))))
												, 0))
								)
				)
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("pos", true)
				.put("parents",
						new Json()
								.put("id", "$parents._id")
								.put("title", "$parents.title")
								.put("lng", "$parents.lng")
								.put("domain", "$parents.domain.value")
								.put("url", "$parents.url")
				)
		));

		pipeline.add(Aggregates.sort(Sorts.ascending("pos")));

		pipeline.add(Aggregates.group("$_id._id", grouper.getGrouper(
				Accumulators.push("parents", "$parents")
		)));

		pipeline.add(Aggregates.project(grouper.getProjection().put("parents",
				new Json("$filter", new Json("input", "$parents").put("as", "parents").put("cond", new Json("$ne", Arrays.asList("$$parents.id", new BsonUndefined()))))
		)));
		return pipeline;
	}

	private static List<Bson> getSistersPipeline(Aggregator grouper) {

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.lookup("Forums", "parents", "parents", "sisters"));


		pipeline.add(Aggregates.project(grouper.getProjection().put("sisters",
				new Json("$filter", new Json("input", "$sisters").put("as", "sisters").put("cond", new Json("$eq", Arrays.asList("$$sisters.lng", "$lng"))))
		)));

		pipeline.add(Aggregates.unwind("$sisters", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.graphLookup("Forums", "$sisters._id", "parents.0", "_id", "sisters.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$sisters.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(new Json("sisters.parents.depth", -1)));

		pipeline.add(Aggregates.group(new Json("sisters_id", "$sisters._id").put("_id", "$_id"),
				grouper.getGrouper(
						Accumulators.push("urls", new Json("$arrayElemAt", Arrays.asList("$sisters.parents.url", 0)))
				)));


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("sisters", new Json("_id", true).put("date", true).put("title", true).put("lng", true).put("position", true)
						.put("url", new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))))
						.put("domain",
								new Json("$arrayElemAt", Arrays.asList(
										new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList("$sisters.lng", "$$domains.key"))))
										, 0))
						)
				)
		));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("sisters",
						new Json("_id", true).put("date", true)
								.put("id", "$sisters._id")
								.put("title", "$sisters.title")
								.put("lng", "$sisters.lng")
								.put("domain", "$sisters.domain.value")
								.put("url", "$sisters.url")
								.put("position", "$sisters.position")
				)
		));


		pipeline.add(Aggregates.sort(Sorts.orderBy(Sorts.ascending("sisters.position"), Sorts.descending("sisters.date"), Sorts.ascending("sisters.id"))));
		pipeline.add(Aggregates.group("$_id._id", grouper.getGrouper(
				Accumulators.push("sisters", "$sisters")
		)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("sisters",
						new Json("$filter", new Json("input", "$sisters").put("as", "sisters").put("cond",
								new Json("$and", Arrays.asList(
										new Json("$ne", Arrays.asList("$$sisters._id", "$_id")),
										new Json("$ne", Arrays.asList("$$sisters._id", new BsonUndefined()))
								))
						))
				)
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("sisters", new Json("title", true).put("url", true).put("id", true).put("lng", true).put("domain", true))
		));
		return pipeline;
	}

	public static List<Bson> getMenuPipeline(Aggregator grouper) {

		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("menu", new Json("$concatArrays", Arrays.asList("$breadcrumb", Arrays.asList(new Json("id", "$_id").put("title", "$title").put("url", "$url").put("order", "$order")))))
		));

		pipeline.add(Aggregates.unwind("$menu", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("menu_index")));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("menu_index", true)
		));


		pipeline.add(Aggregates.lookup("Forums", "menu.id", "parents", "menu.childrens"));

		pipeline.add(Aggregates.unwind("$menu.childrens", new UnwindOptions().preserveNullAndEmptyArrays(true)));


		pipeline.add(Aggregates.graphLookup("Forums", "$menu.childrens._id", "parents.0", "_id", "menu.childrens.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));


		pipeline.add(Aggregates.unwind("$menu.childrens.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.sort(Sorts.descending("menu.childrens.parents.depth")));

		pipeline.add(Aggregates.group(new Json("_id", "$_id").put("menu_childrens_id", "$menu.childrens._id").put("menu_id", "$menu.id"),
				grouper.getGrouper(
						Accumulators.first("menu_index", "$menu_index"),
						Accumulators.push("urls", new Json("$arrayElemAt", Arrays.asList("$menu.childrens.parents.url", 0)))
				))
		);


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("menu_index", true)
				.put("menu", new Json()
						.put("id", true)
						.put("title", true)
						.put("url", true)
						.put("order", true)
						.put("lng", true)
						.put("domain",
								new Json("$arrayElemAt", Arrays.asList(
										new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList("$menu.lng", "$$domains.key"))))
										, 0))
						)
						.put("childrens", new Json("_id", true).put("title", true).put("lng", true)
								.put("domain",
										new Json("$arrayElemAt", Arrays.asList(
												new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList("$menu.childrens.lng", "$$domains.key"))))
												, 0))
								)
								.put("order", new Json("$indexOfArray", Arrays.asList("$menu.order", "$menu.childrens._id")))
								.put("url", new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this"))))))
				)
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("menu_index", true)
				.put("menu", new Json()
						.put("id", "$menu.id")
						.put("title", "$menu.title")
						.put("order", true)
						.put("lng", "$menu.lng")
						.put("domain", "$menu.domain.value")
						.put("url", "$menu.url")
						.put("childrens", new Json()
								.put("_id", true)
								.put("id", "$menu.childrens._id")
								.put("title", "$menu.childrens.title")
								.put("order", true)
								.put("date", true)
								.put("lng", "$menu.childrens.lng")
								.put("domain", "$menu.childrens.domain.value")
								.put("url", "$menu.childrens.url")
						)
				)
		));

		pipeline.add(Aggregates.sort(Sorts.orderBy(Sorts.ascending("menu.childrens.order"), Sorts.ascending("menu.childrens.title"))));

		pipeline.add(Aggregates.group(new Json("_id", "$_id._id").put("menu_id", "$menu.id"),
				grouper.getGrouper(
						Accumulators.first("menu_index", "$menu_index"),
						Accumulators.push("menu_childrens",
								new Json("id", "$menu.childrens._id")
										.put("title", "$menu.childrens.title")
										.put("url", "$menu.childrens.url")
										.put("lng", "$menu.childrens.lng")
										.put("domain", "$menu.childrens.domain")
						)
				)));


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("menu_index", true)
				.put("menu", new Json()
						.put("id", true)
						.put("title", true)
						.put("url", true)
						.put("lng", true)
						.put("domain", true)
						.put("childrens", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$arrayElemAt", Arrays.asList("$menu_childrens.id", 0)), new BsonUndefined())),
								Arrays.asList(), "$menu_childrens")))
				)
		));


		pipeline.add(Aggregates.sort(Sorts.ascending("menu_index")));

		pipeline.add(Aggregates.group("$_id._id",
				grouper.getGrouper(
						Accumulators.push("menu", "$menu")
				))
		);
		return pipeline;
	}

	public static List<Bson> getBreadCrumbPipeline(Aggregator grouper) {
		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.graphLookup("Forums",
				new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$forums", new BsonUndefined())),
						new Json("$arrayElemAt", Arrays.asList("$forums", 0)),
						new Json("$arrayElemAt", Arrays.asList("$parents", 0))
				)), "parents.0", "_id", "breadcrumb", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$breadcrumb", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.graphLookup("Forums", "$breadcrumb._id", "parents.0", "_id", "breadcrumb.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$breadcrumb.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(new Json("breadcrumb.parents.depth", -1)));

		pipeline.add(Aggregates.group(new Json("breadcrumb_id", "$breadcrumb._id").put("_id", "$_id"),
				grouper.getGrouper(
						Accumulators.first("order_", "$order"),
						Accumulators.first("url", new Json("$arrayElemAt", Arrays.asList("$url", 0))),
						Accumulators.push("urls", new Json("$arrayElemAt", Arrays.asList("$breadcrumb.parents.url", 0)))
				))
		);
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("order_", true)
				.put("breadcrumb", new Json("_id", true)
						.put("id", "$breadcrumb._id").put("title", "$breadcrumb.title")
						.put("lng", "$breadcrumb.lng")
						.put("domain",
								new Json("$arrayElemAt", Arrays.asList(
										new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList("$breadcrumb.lng", "$$domains.key"))))
										, 0))
						)

						.put("order", "$breadcrumb.order")
						.put("depth", true).put("urlinit", new Json("$arrayElemAt", Arrays.asList("$breadcrumb.url", 0)))
						.put("url", new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))))
				)
		));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("order_", true)
				.put("breadcrumb", new Json("_id", true)
						.put("id", "$breadcrumb.id")
						.put("title", "$breadcrumb.title")
						.put("lng", "$breadcrumb.lng")
						.put("domain", "$breadcrumb.domain.value")
						.put("url", "$breadcrumb.url")
						.put("order", true)
						.put("depth", true)
						.put("urlinit", true)
				)
		));


		pipeline.add(Aggregates.sort(new Json("breadcrumb.depth", -1)));

		pipeline.add(Aggregates.group("$_id._id",
				grouper.getGrouper(
						Accumulators.first("order_", "$order_"),
						Accumulators.push("urls", "$breadcrumb.urlinit"),
						Accumulators.push("breadcrumb", "$breadcrumb")
				)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("order_", true)
				.put("url",
						new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$arrayElemAt", Arrays.asList("$urls", 0)), null)),
								new Json("$concat", Arrays.asList("/", "$url")),
								new Json("$concat", Arrays.asList(new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))), "/", "$url"))
						))
				)
				.put("breadcrumb", new Json("id", true).put("title", true).put("lng", true).put("domain", true).put("url", true).put("order", true))
		));


		return pipeline;
	}

	public static Json getAllForumRoot(String lng) {

		Aggregator grouper = new Aggregator("id", "title", "meta_title", "text", "url", "domain", "lng", "childrens", "position");

		Bson filter = Filters.and(Filters.eq("lng", lng), Filters.or(Filters.eq("parents", null), Filters.size("parents", 0)));

		List<Bson> pipeline = new ArrayList<>();

		pipeline.addAll(getForumsPipeline(grouper, filter, false, false));

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		pipeline.add(Aggregates.sort(Sorts.orderBy(Sorts.ascending("position"), Sorts.ascending("_id"))));

		return new Json("result", Db.aggregate("Forums", pipeline).into(new ArrayList<>()));
	}

	public static List<Json> getForumsRoot(String url, String lng) {

		Aggregator grouper = new Aggregator("id", "title", "url", "active", "position");

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.match(Filters.and(Filters.eq("lng", lng), Filters.or(Filters.eq("parents", null), Filters.eq("parents", new ArrayList<>())))));

		pipeline.add(Aggregates.sort(Sorts.orderBy(Sorts.ascending("position"), Sorts.descending("date"), Sorts.ascending("_id"))));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("active", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$arrayElemAt", Arrays.asList("$url", 0)), url)), true, false)))
				.put("url", new Json("$concat", Arrays.asList("/", new Json("$arrayElemAt", Arrays.asList("$url", 0)))))));

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		return Db.aggregate("Forums", pipeline).into(new ArrayList<>());
	}

}
