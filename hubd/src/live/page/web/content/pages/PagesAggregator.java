/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.pages;

import com.mongodb.client.model.*;
import live.page.web.content.congrate.RatingsTools;
import live.page.web.content.posts.utils.ThreadsAggregator;
import live.page.web.content.users.UsersAggregator;
import live.page.web.system.Settings;
import live.page.web.system.cosmetic.svg.SVGTemplate;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.db.PipelinerStore;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.db.tags.DbTagsLinker;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class PagesAggregator {

	private static final List<Json> domains = Settings.LANGS_DOMAINS.toList();

	public static Json getPageDomainLng(String url, String lngOrDomain, String paging_str, boolean admin) {
		String lng = null;
		if (Settings.getLangs().contains(lngOrDomain)) {
			lng = lngOrDomain;
		} else {
			lng = Settings.getLang(lngOrDomain);
			if (lng == null) {
				lng = Settings.getLangs().get(0);
			}
		}
		return getPage(url, lng, paging_str, admin);
	}

	public static Json getPage(String url, String lng, String paging_str, boolean admin) {
		if (url == null || lng == null) {
			return null;
		}
		String clean = url.replaceAll(".*/([^/.]+)(/|\\.json|\\.xml|\\.xhtml|\\.html|\\.mob)?$", "$1");
		Json page = PagesAggregator.getPage(Filters.and(Filters.eq("url", clean), Filters.eq("lng", lng)), paging_str, admin);
		if (page == null) {
			Json revision = Db.find("Revisions", Filters.eq("url", clean)).sort(Sorts.descending("edit")).first();
			if (revision == null) {
				try {
					clean = Fx.cleanURL(clean);
				} catch (Exception e) {
					return null;
				}
				page = PagesAggregator.getPage(Filters.and(Filters.eq("url", clean), Filters.eq("lng", lng)), paging_str, admin);
				if (page == null) {
					revision = Db.find("Revisions", Filters.and(Filters.eq("url", clean), Filters.eq("lng", lng))).sort(Sorts.descending("edit")).first();
					if (revision == null) {
						return null;
					}
					return PagesAggregator.getPage(Filters.and(Filters.eq("_id", revision.getString("origine")), Filters.eq("lng", lng)), paging_str, admin);
				}
				return page;
			}
			return PagesAggregator.getPage(Filters.and(Filters.eq("_id", revision.getString("origine")), Filters.eq("lng", lng)), paging_str, admin);
		}
		return page;
	}

	public static Json getPage(Bson filter, String paging_str, boolean admin) {

		Aggregator grouper = new Aggregator(
				"id", "users", "title", "top_title", "intro", "text",
				"docs", "logo", "date", "update", "lng", "domain", "url", "breadcrumb", "parents", "forums",
				"childrens", "sisters", "links", "depth", "position", "urls", "temp_doc", "review", "branch"
		);

		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.match(filter));
		pipeline.add(Aggregates.limit(1));

		pipeline.addAll(RatingsTools.getRatingPipeline(grouper));


		pipeline.add(Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")));
		pipeline.add(Aggregates.lookup("BlobFiles", "docs", "_id", "docs"));
		pipeline.add(Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("docs", new Json("_id", true).put("type", true).put("size", true).put("text", true).put("url", new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$docs._id")))))
		);
		pipeline.add(Aggregates.sort(Sorts.ascending("pos_doc")));
		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(Accumulators.push("docs", "$docs"))));


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("docs", new Json("$filter", new Json("input", "$docs").put("as", "docs").put("cond", new Json("$ne", Arrays.asList("$$docs._id", new BsonUndefined())))))
		));

		pipeline.add(Aggregates.graphLookup("Pages", new Json("$arrayElemAt", Arrays.asList("$parents", 0)), "parents.0", "_id", "breadcrumb", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$breadcrumb", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.graphLookup("Pages", new Json("$arrayElemAt", Arrays.asList("$breadcrumb.parents", 0)), "parents.0", "_id", "breadcrumb.breadcrumb", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.sort(new Json("breadcrumb.depth", -1)));

		pipeline.add(Aggregates.group("$_id",
				grouper.getGrouper(
						Accumulators.push("breadcrumb", "$breadcrumb"),
						Accumulators.push("urls", "$breadcrumb.url")
				)));

		pipeline.add(Aggregates.project(grouper.getProjection()
						.put("logo", new Json("$arrayElemAt", Arrays.asList(
								new Json("$filter", new Json("input", "$docs").put("as", "logo").put("cond", new Json("$eq", Arrays.asList(new Json("$substr", Arrays.asList("$$logo.type", 0, "image/".length())), "image/"))))
								, 0)))
						.put("urls", true)
				)
		);
		pipeline.addAll(DbTagsLinker.getPipeline("text", grouper));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("logo", new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$logo._id")))
				.put("parents_", "$parents")
		));

		//parents

		pipeline.add(Aggregates.lookup("Pages", "parents", "_id", "parents"));
		pipeline.add(Aggregates.unwind("$parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.unwind("$parents.docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")));
		pipeline.add(Aggregates.lookup("BlobFiles", "parents.docs", "_id", "parents.docs"));
		pipeline.add(Aggregates.unwind("$parents.docs", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(Sorts.ascending("pos_doc")));
		pipeline.add(Aggregates.group(new Json("parents_id", "$parents._id").put("_id", "$_id"), grouper.getGrouper(
				Accumulators.push("temp_doc", "$parents.docs"),
				Accumulators.first("parents_", "$parents_")
		)));

		pipeline.add(Aggregates.graphLookup("Pages", new Json("$arrayElemAt", Arrays.asList("$parents.parents", 0)), "parents.0", "_id", "parents.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$parents.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.sort(new Json("parents.parents.depth", -1)));

		pipeline.add(Aggregates.group(new Json("parents_id", "$parents._id").put("_id", "$_id._id"),
				grouper.getGrouper(
						Accumulators.first("parents_", "$parents_"),
						Accumulators.first("parents_url", "$parents.url"),
						Accumulators.push("urls_", "$parents.parents.url")
				)
		));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("temp_doc",
						new Json("$arrayElemAt", Arrays.asList(
								new Json("$filter", new Json("input", "$temp_doc").put("as", "logo").put("cond", new Json("$eq", Arrays.asList(new Json("$substr", Arrays.asList("$$logo.type", 0, "image/".length())), "image/"))))
								, 0))
				)
				.put("pos", new Json("$indexOfArray", Arrays.asList("$parents_", "$parents._id")))
				.put("urls_", true)
				.put("parents_url", true)));

		pipeline.add(Aggregates.project(grouper.getProjection().put("pos", true)
				.put("breadcrumb", true).put("parents", true).put("childrens", true).put("sisters", true).put("urls", true)
				.put("parents", new Json().put("_id", "$parents._id").put("title", "$parents.title").put("top_title", "$parents.top_title").put("intro", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$parents.intro", "")), null, "$parents.intro")))
						.put("logo", new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$temp_doc._id")))
						.put("lng", "$parents.lng")
						.put("domain", getDomainFilter("$parents.lng"))
						.put("url", new Json("$concat", Arrays.asList(new Json("$reduce", new Json("input", "$urls_").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))), "/", "$parents_url"))))
		));
		pipeline.add(Aggregates.project(grouper.getProjection().put("pos", true)
				.put("breadcrumb", true).put("parents", true).put("childrens", true).put("sisters", true).put("urls", true)
				.put("parents", new Json()
						.put("_id", "$parents._id")
						.put("title", "$parents.title")
						.put("top_title", "$parents.top_title")
						.put("intro", "$parents.intro")
						.put("lng", "$parents.lng")
						.put("domain", "$parents.domain")
						.put("url", "$parents.url")
						.put("logo", "$parents.logo")

				)
		));


		pipeline.add(Aggregates.sort(new Json("pos", 1)));

		pipeline.add(Aggregates.group("$_id._id",
				grouper.getGrouper(
						Accumulators.first("childrens_", "$childrens"),
						Accumulators.push("parents", "$parents")
				)));

		//childrens
		pipeline.add(Aggregates.lookup("Pages", "_id", "parents", "childrens"));

		pipeline.add(Aggregates.unwind("$childrens", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.unwind("$childrens.docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")));
		pipeline.add(Aggregates.lookup("BlobFiles", "childrens.docs", "_id", "childrens.docs"));
		pipeline.add(Aggregates.unwind("$childrens.docs", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(Sorts.ascending("pos_doc")));
		pipeline.add(Aggregates.group(new Json("childrens_id", "$childrens._id").put("_id", "$_id"), grouper.getGrouper(
				Accumulators.first("childrens_", "$childrens_"),
				Accumulators.push("temp_doc", "$childrens.docs")
		)));

		pipeline.add(Aggregates.graphLookup("Pages", new Json("$arrayElemAt", Arrays.asList("$childrens.parents", 0)), "parents.0", "_id", "childrens.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$childrens.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.sort(new Json("childrens.parents.depth", -1)));

		pipeline.add(Aggregates.group(new Json("childrens_id", "$childrens._id").put("_id", "$_id._id"), grouper.getGrouper(
				Accumulators.first("childrens_", "$childrens_"),
				Accumulators.first("childrens_url", "$childrens.url"),
				Accumulators.push("urls_", "$childrens.parents.url")
		)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("temp_doc",
						new Json("$arrayElemAt", Arrays.asList(
								new Json("$filter", new Json("input", "$temp_doc").put("as", "logo").put("cond", new Json("$eq", Arrays.asList(new Json("$substr", Arrays.asList("$$logo.type", 0, "image/".length())), "image/"))))
								, 0))
				)
				.put("childrens_", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$childrens_", null)), new ArrayList<>(), "$childrens_")))
				.put("urls_", true)
				.put("childrens_url", true)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("childrens_", true)
				.put("breadcrumb", true).put("parents", true).put("childrens", true).put("sisters", true).put("urls", true)
				.put("childrens", new Json()
						.put("_id", "$childrens._id").put("title", "$childrens.title").put("top_title", "$childrens.top_title").put("intro", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$childrens.intro", "")), null, "$childrens.intro")))
						.put("url", new Json("$concat", Arrays.asList(new Json("$reduce", new Json("input", "$urls_").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))), "/", "$childrens_url")))
						.put("logo", new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$temp_doc._id")))
						.put("lng", "$childrens.lng")
						.put("domain", getDomainFilter("$childrens.lng"))
				)
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("pos", new Json("$indexOfArray", Arrays.asList("$childrens_", "$childrens._id")))
				.put("breadcrumb", true).put("parents", true).put("childrens", true).put("sisters", true).put("urls", true)
				.put("childrens", new Json()
						.put("_id", "$childrens._id")
						.put("title", "$childrens.title")
						.put("top_title", "$childrens.top_title")
						.put("intro", "$childrens.intro")
						.put("lng", "$childrens.lng")
						.put("domain", "$childrens.domain")
						.put("url", "$childrens.url")
						.put("logo", "$childrens.logo")

				)
		));

		pipeline.add(Aggregates.sort(Sorts.orderBy(Sorts.ascending("pos"), Sorts.ascending("childrens.title"))));

		pipeline.add(Aggregates.group("$_id._id", grouper.getGrouper(
				Accumulators.push("childrens", "$childrens")
		)));

		//sisters

		pipeline.add(Aggregates.lookup("Pages", "parents._id", "parents", "sisters"));

		pipeline.add(Aggregates.unwind("$sisters", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos")));

		pipeline.add(Aggregates.unwind("$sisters.docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")));
		pipeline.add(Aggregates.lookup("BlobFiles", "sisters.docs", "_id", "sisters.docs"));
		pipeline.add(Aggregates.unwind("$sisters.docs", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(Sorts.ascending("pos_doc")));
		pipeline.add(Aggregates.group(new Json("sisters_id", "$sisters._id").put("_id", "$_id"), grouper.getGrouper(Accumulators.push("temp_doc", "$sisters.docs"))));

		pipeline.add(Aggregates.graphLookup("Pages", new Json("$arrayElemAt", Arrays.asList("$sisters.parents", 0)), "parents.0", "_id", "sisters.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$sisters.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(new Json("sisters.parents.depth", -1)));

		pipeline.add(Aggregates.group(new Json("sisters_id", "$sisters._id").put("_id", "$_id._id"), grouper.getGrouper(
				Accumulators.first("pos", "$pos"),
				Accumulators.first("sisters_url", "$sisters.url"),
				Accumulators.push("urls_", "$sisters.parents.url")
		)));

		pipeline.add(Aggregates.project(grouper.getProjection().put("pos", true)
				.put("temp_doc",
						new Json("$arrayElemAt", Arrays.asList(
								new Json("$filter", new Json("input", "$temp_doc").put("as", "logo").put("cond", new Json("$eq", Arrays.asList(new Json("$substr", Arrays.asList("$$logo.type", 0, "image/".length())), "image/"))))
								, 0))
				)
				.put("urls_", true)
				.put("sisters_url", true)));

		pipeline.add(Aggregates.project(grouper.getProjection().put("pos", true)
				.put("breadcrumb", true).put("parents", true).put("childrens", true).put("sisters", true).put("urls", true)
				.put("sisters", new Json().put("_id", "$sisters._id").put("title", "$sisters.title").put("top_title", "$sisters.top_title").put("intro", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$sisters.intro", "")), null, "$sisters.intro")))
						.put("lng", "$sisters.lng")
						.put("domain", getDomainFilter("$sisters.lng"))
						.put("url", new Json("$concat", Arrays.asList(new Json("$reduce", new Json("input", "$urls_").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))), "/", "$sisters_url")))
						.put("logo", new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$temp_doc._id")))

				)
		));
		pipeline.add(Aggregates.project(grouper.getProjection().put("pos", true)
				.put("breadcrumb", true).put("parents", true).put("childrens", true).put("sisters", true).put("urls", true)
				.put("sisters", new Json()
						.put("_id", "$sisters._id")
						.put("title", "$sisters.title")
						.put("top_title", "$sisters.top_title")
						.put("intro", "$sisters.intro")
						.put("lng", "$sisters.lng")
						.put("domain", "$sisters.domain")
						.put("url", "$sisters.url")
						.put("logo", "$sisters.logo")

				))
		);

		pipeline.add(Aggregates.sort(Sorts.orderBy(Sorts.descending("pos"), Sorts.ascending("sisters.title"))));

		pipeline.add(Aggregates.group("$_id._id", grouper.getGrouper(
				Accumulators.push("sisters", "$sisters")
		)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("sisters", new Json("$filter", new Json("input", "$sisters").put("as", "sisters").put("cond", new Json("$ne", Arrays.asList("$$sisters._id", "$_id")))))
		));


		//breadcrumb

		pipeline.add(Aggregates.unwind("$breadcrumb", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("position")));

		pipeline.add(Aggregates.unwind("$breadcrumb.docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")));
		pipeline.add(Aggregates.lookup("BlobFiles", "breadcrumb.docs", "_id", "breadcrumb.docs"));
		pipeline.add(Aggregates.unwind("$breadcrumb.docs", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(Sorts.ascending("pos_doc")));
		pipeline.add(Aggregates.group(new Json("breadcrumb_id", "$breadcrumb._id").put("_id", "$_id"), grouper.getGrouper(Accumulators.push("temp_doc", "$breadcrumb.docs"))));

		pipeline.add(Aggregates.graphLookup("Pages", new Json("$arrayElemAt", Arrays.asList("$breadcrumb.parents", 0)), "parents.0", "_id", "breadcrumb.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$breadcrumb.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(new Json("breadcrumb.parents.depth", -1)));

		pipeline.add(Aggregates.group(new Json("breadcrumb_id", "$breadcrumb._id").put("_id", "$_id._id"), grouper.getGrouper(
				Accumulators.first("breadcrumb_url", "$breadcrumb.url"),
				Accumulators.push("urls_", "$breadcrumb.parents.url")
		)));

		pipeline.add(Aggregates.sort(new Json("position", 1)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("temp_doc",
						new Json("$arrayElemAt", Arrays.asList(
								new Json("$filter", new Json("input", "$temp_doc").put("as", "logo").put("cond", new Json("$eq", Arrays.asList(new Json("$substr", Arrays.asList("$$logo.type", 0, "image/".length())), "image/"))))
								, 0))
				)
				.put("urls_", true)
				.put("breadcrumb_url", true)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("breadcrumb", true).put("parents", true).put("childrens", true).put("sisters", true).put("links", true).put("urls", true)
				.put("breadcrumb", new Json().put("_id", "$breadcrumb._id").put("title", "$breadcrumb.title").put("top_title", "$breadcrumb.top_title").put("intro", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$breadcrumb.intro", "")), null, "$breadcrumb.intro")))
						.put("url", new Json("$concat", Arrays.asList(new Json("$reduce", new Json("input", "$urls_").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))), "/", "$breadcrumb_url")))
						.put("lng", "$breadcrumb.lng")
						.put("domain", getDomainFilter("$breadcrumb.lng"))
						.put("logo", new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$temp_doc._id")))
				)
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("breadcrumb", true).put("parents", true).put("childrens", true).put("sisters", true).put("links", true).put("urls", true)
				.put("breadcrumb", new Json()
						.put("_id", "$breadcrumb._id")
						.put("title", "$breadcrumb.title")
						.put("top_title", "$breadcrumb.top_title")
						.put("intro", "$breadcrumb.intro")
						.put("lng", "$breadcrumb.lng")
						.put("domain", "$breadcrumb.domain")
						.put("url", "$breadcrumb.url")
						.put("logo", "$breadcrumb.logo")

				)

		));

		pipeline.add(Aggregates.group("$_id._id", grouper.getGrouper(
				Accumulators.first("forums_", "$forums"),
				Accumulators.push("breadcrumb", "$breadcrumb")
		)));
		///<Forums>

		pipeline.add(Aggregates.lookup("Forums", "forums", "_id", "forums"));
		pipeline.add(Aggregates.unwind("$forums", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.graphLookup("Forums", "$forums._id", "parents.0", "_id", "forums.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));
		pipeline.add(Aggregates.graphLookup("Forums", "$forums._id", "_id", "parents", "forums.branch", new GraphLookupOptions().maxDepth(5000)));

		pipeline.add(Aggregates.unwind("$forums.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.sort(Sorts.descending("forums.parents.depth")));

		pipeline.add(Aggregates.group(new Json("forums_id", "$forums._id").put("_id", "$_id"), grouper.getGrouper(
				Accumulators.first("forums_", "$forums_"),
				Accumulators.push("urlsforum", new Json("$arrayElemAt", Arrays.asList("$forums.parents.url", 0)))
		)));
		//BRANCHE
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("forums", new Json()
						.put("id", "$forums._id")
						.put("title", "$forums.title")
						.put("url", new Json("$reduce", new Json("input", "$urlsforum").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))))
						.put("lng", "$forums.lng")
						.put("domain", getDomainFilter("$forums.lng"))
						.put("branch",
								new Json("$concatArrays", Arrays.asList(
										Arrays.asList(new Json("_id", "$forums._id")),
										new Json("$filter", new Json("input", "$forums.branch").put("as", "branch").put("cond", new Json("$ne", Arrays.asList("$$branch._id", new BsonUndefined()))))
								))
						)
				)
				.put("pos", new Json("$indexOfArray", Arrays.asList("$forums_", "$forums._id")))
		));

		pipeline.add(Aggregates.unwind("$forums.branch", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.project(grouper.getProjection().put("pos", true)
				.put("forums", new Json()
						.put("id", true)
						.put("title", true)
						.put("lng", true)
						.put("domain", true)
						.put("url", true)
						.put("branch",
								new Json("$concat", Arrays.asList("Forums(", "$forums.branch._id", ")"))

						)
				)
		));
		pipeline.add(Aggregates.group(new Json("forums_id", "$forums.id").put("_id", "$_id._id"), grouper.getGrouper(
				Accumulators.first("pos", "$pos"),
				Accumulators.push("branch", "$forums.branch")
		)));

		pipeline.add(Aggregates.project(grouper.getProjection()

				.put("pos", true)
				.put("forums", new Json()
						.put("id", "$forums.id")
						.put("title", "$forums.title")
						.put("lng", "$forums.lng")
						.put("domain", "$forums.domain")
						.put("url", "$forums.url")

				)
		));

		///


		pipeline.add(Aggregates.sort(Sorts.ascending("pos")));


		pipeline.add(Aggregates.group("$_id._id", grouper.getGrouper(
				Accumulators.push("forums", "$forums"),
				Accumulators.push("branch", "$branch")
				))
		);
		pipeline.add(Aggregates.project(grouper.getProjection().put("branch",
				new Json("$reduce", new Json("input", "$branch").put("initialValue", Arrays.asList(new Json("$concat", Arrays.asList("Pages(", "$_id", ")")))).put("in", new Json("$setUnion", Arrays.asList("$$value", "$$this")))))));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("forums",
						new Json("$filter", new Json("input", "$forums").put("as", "forums").put("cond", new Json("$ne", Arrays.asList("$$forums.id", new BsonUndefined()))))
				)
				.put("branch",
						new Json("$filter", new Json("input", "$branch").put("as", "branch").put("cond",
								new Json("$and", Arrays.asList(
										new Json("$ne", Arrays.asList("$$branch", new BsonUndefined())),
										new Json("$ne", Arrays.asList("$$branch", null))
								))
						))
				)
		));

		//</Forums>

		//<users>

		pipeline.addAll(UsersAggregator.getUserPipeline(grouper, true));

		//</users>

		pipeline.add(Aggregates.project(grouper.getProjection().put("_id", false).remove("urls").put("id", "$_id")
				.put("text", new Json("$cond", Arrays.asList(new Json("$or", Arrays.asList(new Json("$eq", Arrays.asList("$text", "")), new Json("$eq", Arrays.asList("$text", "\n")))), null, "$text")))
				.put("intro", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$intro", "")), null, "$intro")))
				.put("breadcrumb", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$arrayElemAt", Arrays.asList("$breadcrumb.url", 0)), null)), new ArrayList<>(), "$breadcrumb")))
				.put("childrens", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$arrayElemAt", Arrays.asList("$childrens.url", 0)), null)), new ArrayList<>(), "$childrens")))
				.put("sisters", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$arrayElemAt", Arrays.asList("$sisters.url", 0)), null)), new ArrayList<>(), "$sisters")))
				.put("parents", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$arrayElemAt", Arrays.asList("$parents.url", 0)), null)), new ArrayList<>(), "$parents")))
				.put("links", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$arrayElemAt", Arrays.asList("$links.url", 0)), null)), new ArrayList<>(), "$links")))
				.put("domain", getDomainFilter("$lng"))
				.put("url", new Json("$concat", Arrays.asList(new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))), "/", "$url")))
		));

		if (!admin) {

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("parents", new Json("$filter", new Json("input", "$parents").put("as", "parents").put("cond", new Json("$not", new Json("$in", Arrays.asList("$$parents._id", "$childrens._id"))))))
					.put("sisters", new Json("$filter", new Json("input", "$sisters").put("as", "sisters").put("cond", new Json("$not", new Json("$or", Arrays.asList(
							new Json("$eq", Arrays.asList("$$sisters._id", "$id")), new Json("$in", Arrays.asList("$$sisters._id", "$parents._id")), new Json("$in", Arrays.asList("$$sisters._id", "$childrens._id"))))))))
			));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("parents", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$arrayElemAt", Arrays.asList("$parents._id", 0)), new BsonUndefined())), new ArrayList<>(), "$parents")))

			));

		}

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("branch", new Json("$concatArrays", Arrays.asList(Arrays.asList(new Json("$concat", Arrays.asList("Pages(", "$_id", ")"))), "$branch"))))
		);

		pipeline.add(Aggregates.project(new Json()
				.put("id", "$id")
				.put("title", "$title")
				.put("lng", "$lng")
				.put("domain", "$domain")
				.put("url", "$url")
				.put("logo", "$logo")
				.put("users", "$users")
				.put("top_title", "$top_title")
				.put("date", "$date")
				.put("update", "$update")
				.put("intro", "$intro")
				.put("review", "$review")
				.put("text", "$text")
				.put("docs", "$docs")
				.put("breadcrumb", "$breadcrumb")
				.put("childrens", "$childrens")
				.put("parents", "$parents")
				.put("sisters", "$sisters")
				.put("links", "$links")
				.put("forums", "$forums")
				.put("branch", "$branch")
		));

		Json page = Db.aggregate("Pages", pipeline).first();
		if (page == null) {
			return null;
		}
		page.put("threads", ThreadsAggregator.getThreads(Filters.in("parents", page.getList("branch")), paging_str, false));
		page.remove("branch");

		return page;

	}

	public static Json getPages(Bson filter, int limit, String next_str) {

		Paginer paginer = new Paginer(next_str, "-update", limit);

		Aggregator grouper = new Aggregator("id", "title", "logo", "top_title", "date", "update", "intro", "lng", "domain", "url", "breadcrumb");

		List<Bson> pipeline = new ArrayList<>();
		List<Bson> filters = new ArrayList<>();
		if (filter != null) {
			filters.add(filter);
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

		pipeline.add(Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")));
		pipeline.add(Aggregates.lookup("BlobFiles", "docs", "_id", "docs"));
		pipeline.add(Aggregates.sort(Sorts.ascending("pos_doc")));
		pipeline.add(Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(
				Accumulators.first("parents", "$parents"),
				Accumulators.push("docs", "$docs")
		)));

		pipeline.add(Aggregates.graphLookup("Pages", new Json("$arrayElemAt", Arrays.asList("$parents", 0)), "parents.0", "_id", "breadcrumb", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$breadcrumb", new UnwindOptions().preserveNullAndEmptyArrays(true)));


		pipeline.add(Aggregates.graphLookup("Pages", new Json("$arrayElemAt", Arrays.asList("$breadcrumb.parents", 0)), "parents.0", "_id", "breadcrumb.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$breadcrumb.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(new Json("breadcrumb.parents.depth", -1)));

		pipeline.add(Aggregates.group(new Json("bid", "$breadcrumb._id").put("id", "$_id"),
				grouper.getGrouper(
						Accumulators.first("id", "$_id"),
						Accumulators.first("docs", "$docs"),
						Accumulators.push("urls_", "$breadcrumb.parents.url")
				))
		);

		pipeline.add(Aggregates.sort(new Json("breadcrumb.depth", -1)));

		pipeline.add(Aggregates.project(grouper.getProjection()
						.put("logo", new Json("$arrayElemAt", Arrays.asList(
								new Json("$filter", new Json("input", "$docs").put("as", "logo").put("cond", new Json("$eq", Arrays.asList(new Json("$substr", Arrays.asList("$$logo.type", 0, "image/".length())), "image/"))))
								, 0)))
						.put("breadcrumb", new Json().put("_id", true).put("title", true).put("lng", true)

								.put("domain", getDomainFilter("$breadcrumb.lng"))
								.put("url", true))
						.put("urls_", true)
						.put("urls_", true)
						.put("lng", true)
						.put("domain", getDomainFilter("$lng"))
				)
		);
		pipeline.add(Aggregates.project(grouper.getProjection()
						.put("logo", new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$logo._id")))
						.put("breadcrumb",
								new Json().put("id", "$breadcrumb._id").put("title", "$breadcrumb.title").put("init_url", "$breadcrumb.url")
										.put("lng", "$breadcrumb.lng")
										.put("domain", "$breadcrumb.domain")
										.put("url", new Json("$concat", Arrays.asList(new Json("$reduce", new Json("input", "$urls_").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))), "/", "$breadcrumb.url")))
						)
						.put("domain", "$domain")
				)
		);


		pipeline.add(Aggregates.sort(new Json("breadcrumb.depth", -1)));

		pipeline.add(Aggregates.group("$id", grouper.getGrouper(
				Accumulators.push("breadcrumb", "$breadcrumb"),
				Accumulators.push("urls", "$breadcrumb.init_url")
				)
		));


		pipeline.add(Aggregates.project(grouper.getProjection().remove("id")
				.put("top_title", new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$top_title", "")), "$top_title", null)))
				.put("intro", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$intro", "")), null, "$intro")))
				.put("url", new Json("$concat", Arrays.asList(new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))), "/", "$url")))
				.put("breadcrumb", new Json().put("id", true).put("title", true).put("lng", true).put("domain", true).put("url", true))
		));

		pipeline.add(Aggregates.project(grouper.getProjection()
						.put("breadcrumb", new Json("$filter", new Json("input", "$breadcrumb").put("as", "breadcrumb").put("cond", new Json("$ne", Arrays.asList("$$breadcrumb.id", new BsonUndefined())))))
				)
		);
		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		pipeline.add(paginer.getLastSort());

		return paginer.getResult("Pages", pipeline);
	}


	public static List<Json> getOrph() {
		return Db.aggregate("Pages", Arrays.asList(
				Aggregates.lookup("Pages", "parents", "_id", "parents"),
				Aggregates.unwind("$parents", new UnwindOptions().preserveNullAndEmptyArrays(true)),
				Aggregates.match(Filters.eq("parents", null)),
				Aggregates.project(new Json("_id", true).put("title", true).put("url", true))
		)).into(new ArrayList<>());
	}


	private static Json getDomainFilter(String lng) {
		return new Json("$let",
				new Json(
						"vars", new Json("domain",
						new Json("$arrayElemAt", Arrays.asList(
								new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList(lng, "$$domains.key"))))
								, 0))
				)).put(
						"in", "$$domain.value"
				));
	}

	public static List<Bson> getPagesLookup(String localField, String foreignField, Aggregator grouper, String sort_array) {

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.lookup("Pages", localField, foreignField, "pages"));

		pipeline.add(Aggregates.unwind("$pages", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.unwind("$pages.docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")));
		pipeline.add(Aggregates.lookup("BlobFiles", "pages.docs", "_id", "pages.docs"));
		pipeline.add(Aggregates.unwind("$pages.docs", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(Sorts.ascending("pos_doc")));

		pipeline.add(Aggregates.group(new Json("_id", "$_id").put("page", "$pages._id"), grouper.getGrouper(
				Accumulators.first(sort_array.replace(".", "_"), "$" + sort_array),
				Accumulators.push("temp_doc", "$pages.docs"))));

		pipeline.add(Aggregates.graphLookup("Pages", "$pages._id", "parents.0", "_id", "pages.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$pages.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.sort(new Json("pages.parents.depth", -1)));

		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(
				Accumulators.first(sort_array.replace(".", "_"), "$" + sort_array.replace(".", "_")),
				Accumulators.first("temp_doc", "$temp_doc"),
				Accumulators.push("urls_pages", "$pages.parents.url")
		)));


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put(sort_array.replace(".", "_"), true)
				.put("urls_pages", true)
				.put("temp_doc",
						new Json("$arrayElemAt", Arrays.asList(
								new Json("$filter", new Json("input", "$temp_doc").put("as", "temp_doc").put("cond", new Json("$eq", Arrays.asList(new Json("$substr", Arrays.asList("$$temp_doc.type", 0, "image/".length())), "image/"))))
								, 0))
				)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("pages", new Json("_id", true)
						.put("lng", true)
						.put("domain",
								new Json("$arrayElemAt", Arrays.asList(
										new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList("$pages.lng", "$$domains.key"))))
										, 0))
						)
						.put("order", new Json("$indexOfArray", Arrays.asList("$" + sort_array.replace(".", "_"), "$pages._id")))
						.put("title", "$pages.title")
						.put("top_title", "$pages.top_title")
						.put("intro", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$pages.intro", "")), null, "$pages.intro")))
						.put("logo", new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$temp_doc._id")))
						.put("url", new Json("$reduce", new Json("input", "$urls_pages").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this"))))))
		));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("pages", new Json("id", "$pages._id")
						.put("order", "$pages.order")
						.put("title", "$pages.title")
						.put("top_title", "$pages.top_title")
						.put("intro", "$pages.intro")
						.put("logo", "$pages.logo")
						.put("lng", "$pages.lng")
						.put("domain", "$pages.domain.value")
						.put("url", "$pages.url")
				)
		));

		pipeline.add(Aggregates.sort(Sorts.ascending("pages.order")));


		pipeline.add(Aggregates.group("$_id._id", grouper.getGrouper(
				Accumulators.push("pages", "$pages")
		)));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("pages",
						new Json("$filter", new Json("input", "$pages").put("as", "pages").put("cond", new Json("$ne", Arrays.asList("$$pages.id", new BsonUndefined()))))
				)
		));
		return pipeline;
	}

	public static List<Json> getSitemapPages(Date date, String lng, int limit) {

		Aggregator grouper = new Aggregator("id", "date", "update", "lng", "domain", "url");

		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.match(Filters.and(Filters.eq("lng", lng),
				Filters.gte("date", date))));

		pipeline.add(Aggregates.sort(Sorts.ascending("date")));
		pipeline.add(Aggregates.limit(limit));

		pipeline.add(Aggregates.graphLookup("Pages", new Json("$arrayElemAt", Arrays.asList("$parents", 0)), "parents.0", "_id", "breadcrumb", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$breadcrumb", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.sort(new Json("breadcrumb.depth", -1)));

		pipeline.add(Aggregates.group("$_id",
				grouper.getGrouper(
						Accumulators.push("urls", "$breadcrumb.url")
				))
		);

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("url", new Json("$concat", Arrays.asList(new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))), "/", "$url")))
				.put("update", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$update", new BsonUndefined())), "$date", "$update")))
				.put("domain",
						new Json("$arrayElemAt", Arrays.asList(
								new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList("$lng", "$$domains.key"))))
								, 0))
				)
		));

		pipeline.add(Aggregates.project(grouper.getProjection().put("domain", "$domain.value")));
		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		pipeline.add(Aggregates.sort(Sorts.descending("update")));
		return Db.aggregate("Pages", pipeline).into(new ArrayList<>());


	}

	public static class PagesPipelines extends PipelinerStore.Pipeliner {


		public PagesPipelines(String type, String lng, Paginer paginer) {
			super(type, paginer);
			addFilter(Filters.eq("lng", lng));
		}

		@Override
		protected List<Bson> getSearchPipeline() {

			Aggregator grouper = new Aggregator("date", "title", "intro", "score", "text", "svg", "logo", "docs", "breadcrumb", "urls", "url", "parents", "tag");

			List<Bson> pipeline = new ArrayList<>();

			pipeline.add(Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true)));
			pipeline.add(Aggregates.lookup("BlobFiles", "docs", "_id", "logo"));

			pipeline.add(Aggregates.group("$_id", grouper.getGrouper(Accumulators.push("logo", new Json("$arrayElemAt", Arrays.asList("$logo", 0))))));


			pipeline.add(Aggregates.graphLookup("Pages", new Json("$arrayElemAt", Arrays.asList("$parents", 0)), "parents.0", "_id", "breadcrumb", new GraphLookupOptions().depthField("depth").maxDepth(50)));
			pipeline.add(Aggregates.unwind("$breadcrumb", new UnwindOptions().preserveNullAndEmptyArrays(true)));

			pipeline.add(new Json("$sort", new Json("breadcrumb.depth", -1)));

			pipeline.add(Aggregates.group("$_id",
					grouper.getGrouper(
							Accumulators.push("urls", "$breadcrumb.url"),
							Accumulators.push("breadcrumb", "$breadcrumb")
					))
			);

			pipeline.add(Aggregates.project(grouper.getProjection().put("breadcrumb", new Json("title", true).put("_id", true).put("depth", true).put("url", true).put("length", new Json("$size", "$breadcrumb")).put("urls", "$breadcrumb.url"))));

			pipeline.add(Aggregates.unwind("$breadcrumb", new UnwindOptions().preserveNullAndEmptyArrays(true)));
			pipeline.add(Aggregates.project(grouper.getProjection().put("breadcrumb", new Json("title", true).put("_id", true).put("url_init", "$breadcrumb.url").put("urls", true).put("length", new Json("$subtract", Arrays.asList(new Json("$subtract", Arrays.asList("$breadcrumb.length", "$breadcrumb.depth")), 0))))));

			pipeline.add(Aggregates.project(grouper.getProjection().put("breadcrumb", new Json("title", true).put("_id", true).put("url_init", true).put("length", true).put("urls", new Json("$cond", Arrays.asList(new Json("$gt", Arrays.asList("$breadcrumb.length", 0)), new Json("$slice", Arrays.asList("$breadcrumb.urls", 0, "$breadcrumb.length")), new Json("$slice", Arrays.asList("$breadcrumb.urls", 0, 1))))))));
			pipeline.add(Aggregates.project(grouper.getProjection().put("breadcrumb", new Json("title", true).put("_id", true).put("url_init", true).put("url", new Json("$concat", Arrays.asList(new Json("$reduce", new Json("input", "$breadcrumb.urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this"))))))))));

			pipeline.add(Aggregates.group("$_id",
					grouper.getGrouper(
							Accumulators.push("urls", "$breadcrumb.url_init"),
							Accumulators.push("breadcrumb", "$breadcrumb")
					)));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("breadcrumb", new Json("$filter", new Json("input", "$breadcrumb").put("as", "breadcrumb").put("cond", new Json("$ne", Arrays.asList("$$breadcrumb._id", new BsonUndefined())))))
			));
			pipeline.add(Aggregates.project(grouper.getProjection().put("breadcrumb", new Json("title", true).put("url", true))));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("logo",
							new Json("$filter", new Json("input", "$logo").put("as", "logo").put("cond",
									new Json("$eq", Arrays.asList(
											new Json("$substrCP", Arrays.asList("$$logo.type", 0, "image".length())), "image")
									)
							))
					)
			));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("logo", new Json("$cond", Arrays.asList(
							new Json("$gt", Arrays.asList(new Json("$size", "$logo"), 0)),
							new Json("$arrayElemAt", Arrays.asList("$logo", 0)), null)
					))
			));


			pipeline.add(Aggregates.project(grouper.getProjection()
							.put("logo", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$logo", null)), null, new Json("$concat", Arrays.asList(Settings.getCDNHttp(), "/files/", "$logo._id")))))
							.put("url", new Json("$concat", Arrays.asList(new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))), "/", "$url")))
							.put("tag", new Json("$concat", Arrays.asList("Pages(", "$_id", ")")))

							.put("svg", SVGTemplate.get("fa_icon_newspaper_o"))
					)
			);


			return pipeline;
		}

		@Override
		public List<Bson> getUrlDbTags(Aggregator grouper, String key) {
			List<Bson> pipeline = new ArrayList<>();

			pipeline.add(Aggregates.graphLookup("Pages", "$" + key + "._id", "parents.0", "_id", "breadcrumblinks", new GraphLookupOptions().depthField("depth").maxDepth(50)));
			pipeline.add(Aggregates.unwind("$breadcrumblinks", new UnwindOptions().preserveNullAndEmptyArrays(true)));
			pipeline.add(new Json("$sort", new Json("breadcrumblinks.depth", -1)));
			pipeline.add(Aggregates.group(new Json("_id", "$_id").put(key, "$" + key + "._id"),
					grouper.getGrouper(
							Accumulators.push("urlslinks", "$breadcrumblinks.url")
					))
			);

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("_id", "$_id._id")
					.put(key, new Json()
							.put("_id", true)
							.put("top_title", true)
							.put("title", true)
							.put("url", new Json("$reduce", new Json("input", "$urlslinks").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))))
							.put("domain", getDomainFilter("$" + key + ".lng"))
					)
			));


			return pipeline;
		}

	}
}
