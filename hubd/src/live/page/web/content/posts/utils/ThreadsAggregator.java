/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.posts.utils;

import com.mongodb.client.model.*;
import live.page.web.blobs.BlobsDb;
import live.page.web.content.congrate.RatingsTools;
import live.page.web.content.pages.PagesAggregator;
import live.page.web.content.users.UsersAggregator;
import live.page.web.system.Settings;
import live.page.web.system.cosmetic.svg.SVGTemplate;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.db.PipelinerStore;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.db.tags.DbTags;
import live.page.web.system.db.tags.DbTagsLinker;
import live.page.web.system.json.Json;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Hidder;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class ThreadsAggregator {

	private static final int number_posts = 15;
	private static final int number_threads = 30;

	private static final List<Json> domains = Settings.LANGS_DOMAINS.toList();

	public static Json getThread(String _id, Users user, String paging_str, boolean remove) {
		List<Bson> pipeline = new ArrayList<>();

		Aggregator grouper = new Aggregator("index", "user", "sysid", "date", "last", "title", "review", "coins", "url", "domain", "lng",
				"breadcrumb", "text", "comments", "changes", "docs", "link", "links", "remove", "replies",
				"parents", "menu", "forums", "pages", "branch", "posts");

		Bson filter = remove ? Filters.and(Filters.eq("_id", _id), Filters.exists("remove", false)) : Filters.eq("_id", _id);
		pipeline.add(Aggregates.match(filter));
		pipeline.add(Aggregates.limit(1));

		pipeline.add(Aggregates.project(grouper.getProjection()
						.put("coins", new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$coins", new BsonUndefined())), new Json("$size", "$coins"), 0)))
						.put("forums", new Json("$filter", new Json("input", "$parents").put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$substrCP", Arrays.asList("$$ele", 0, "Forums(".length())), "Forums(")))))
						.put("pages", new Json("$filter", new Json("input", "$parents").put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$substrCP", Arrays.asList("$$ele", 0, "Pages(".length())), "Pages(")))))
				)
		);

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("forums", new Json("$map", new Json("input", "$forums").put("as", "ele").put("in", new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList(new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList("$$ele", "(")), 1)), ")")), 0)))))
				.put("pages", new Json("$map", new Json("input", "$pages").put("as", "ele").put("in", new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList(new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList("$$ele", "(")), 1)), ")")), 0)))))
		));

		pipeline.addAll(getBranchePipeline(grouper));


		pipeline.addAll(RatingsTools.getRatingPipeline(grouper));

		pipeline.add(Aggregates.project(grouper.getProjection().put("pages_order", "$pages")));

		pipeline.addAll(PagesAggregator.getPagesLookup("pages", "_id", grouper, "pages_order"));

		pipeline.addAll(getCommentsPipeline(grouper, remove));


		pipeline.addAll(BlobsDb.getBlobsPipeline(grouper));


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("parents", "$forums")
		));

		pipeline.addAll(getParentPipeline(grouper, remove));


		pipeline.addAll(UsersAggregator.getUserPipeline(grouper, false));

		pipeline.addAll(ForumsAggregator.getParentsPipeline(grouper));

		pipeline.addAll(ForumsAggregator.getMenuPipeline(grouper));


		pipeline.addAll(DbTagsLinker.getPipeline("text", grouper));

		pipeline.add(Aggregates.project(grouper.getProjection().put("forums", "$parents").remove("parents")));

		Paginer paginer = new Paginer(paging_str, "date", number_posts);

		pipeline.add(new Json("$lookup", new Json("from", "Posts").put("as", "posts")
				.put("pipeline", getThreadPostsPipeline(user, Filters.eq("parents", "Posts(" + _id + ")"), paginer, remove))
		));

/*
		pipeline.add(new live.page.web.utils.json.Json("$lookup", new live.page.web.utils.json.Json("from", "Posts").put("as", "branch").put("let", new live.page.web.utils.json.Json("branch", "$branch"))
				.put("pipeline", ThreadsAggregator.getThreadsPipeline(
						Filters.expr(
								new live.page.web.utils.json.Json("$in", Arrays.asList(new live.page.web.utils.json.Json("$arrayElemAt", Arrays.asList("$parents", 0)), "$$branch"))
						)
						, grouper, new Paginer(null, "-date", 50), remove))
		));
*/
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("breadcrumb", new Json()
						.put("id", true)
						.put("title", true)
						.put("lng", true)
						.put("domain", true)
						.put("url", true)))
		);
		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		Json thread = Db.aggregate("Posts", pipeline).first();
		if (thread == null) {
			return null;
		}
		thread.put("posts", paginer.getResult(thread.getListJson("posts")));


		// component index... ?
		if (thread.getList("branch").size() > 0) {
			thread.put("branch", ThreadsAggregator.getThreads(
					Filters.and(
							Filters.or(
									Filters.eq("index", true),
									Filters.gt("replies", 0)
							),
							Filters.exists("remove", false),
							Filters.and(
									Filters.ne("_id", _id),
									Filters.in("parents", thread.getList("branch")))
					), new Paginer(paging_str, "-date", 50), remove).getListJson("result")
			);
		}

		return thread;
	}

	public static List<Bson> getBranchePipeline(Aggregator grouper) {

		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.graphLookup("Forums", "$forums", "_id", "parents", "branch", new GraphLookupOptions().maxDepth(1000)));


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("branch", new Json("$concatArrays", Arrays.asList("$branch", new Json("$map", new Json("input", "$forums").put("as", "ele").put("in", new Json("_id", "$$ele"))))))
		));
		pipeline.add(Aggregates.unwind("$branch", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("branch", new Json("$concat", Arrays.asList("Forums(", "$branch._id", ")")))
		));

		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(
				Accumulators.push("branch", "$branch")
		)));
		return pipeline;

	}

	public static Json getThreads(Bson filter, String paging_str, boolean remove) {
		Paginer paginer = new Paginer(paging_str, "-last.date", number_threads);
		return getThreads(filter, paginer, remove);
	}

	private static Json getThreads(Bson filter, Paginer paginer, boolean remove) {

		Aggregator grouper = new Aggregator("id", "index", "remove", "date", "last", "post", "title", "text", "link", "user", "replies", "lng", "domain", "url", "breadcrumb", "review", "forums");

		return paginer.getResult("Posts", getThreadsPipeline(filter, grouper, paginer, remove));
	}

	public static Json getPost(String post_id, Users user) {

		Json post = Db.findById("Posts", post_id);
		if (post == null) {
			return null;
		}
		DbTags thread = null;
		for (DbTags parent : post.getParents("parents")) {
			if (parent.getCollection().equals("Posts")) {
				thread = parent;
			}
		}

		if (thread == null) {
			thread = new DbTags("Posts", post.getId());
		}

		int limitbefore = number_posts - (number_posts / 3);
		List<Json> before = Db.find("Posts",
				Filters.and(
						Filters.exists("remove", false),
						Filters.in("parents", thread.toString()),
						Filters.or(
								Filters.lt("date", post.getDate("date")),
								Filters.and(
										Filters.eq("date", post.getDate("date")),
										Filters.gte("_id", post.getId())
								)
						)
				)
		).limit(limitbefore).sort(Sorts.orderBy(Sorts.descending("date"), Sorts.ascending("_id"))).projection(new Json("_id", true).put("date", true)).into(new ArrayList<>());

		String paging_str = "first";
		if (before != null && before.size() == limitbefore) {
			paging_str = Hidder.encodeJson(before.get(before.size() - 1).prepend("@", 1));
		}

		return getThread(thread.getId(), user, paging_str, false);
	}

	private static List<Bson> getThreadsPipeline(Bson filter, Aggregator grouper, Paginer paginer, boolean remove) {

		List<Bson> pipeline = new ArrayList<>();

		List<Bson> filters = new ArrayList<>();
		if (filter != null) {
			filters.add(filter);
		}
		if (!remove) {
			filters.add(Filters.exists("remove", false));
		}

		Bson paging = paginer.getFilters();
		if (paging != null) {
			filters.add(paging);
		}

		pipeline.add(Aggregates.match(Filters.and(filters)));
		pipeline.add(paginer.getFirstSort());

		pipeline.add(paginer.getLimit());


		pipeline.add(Aggregates.project(grouper.getProjection()
						.put("forums", new Json("$filter", new Json("input", "$parents").put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$substrCP", Arrays.asList("$$ele", 0, "Forums(".length())), "Forums(")))))
						.put("pages", new Json("$filter", new Json("input", "$parents").put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$substrCP", Arrays.asList("$$ele", 0, "Pages(".length())), "Pages(")))))
				)
		);

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("forums", new Json("$map", new Json("input", "$" + "forums").put("as", "ele").put("in", new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList(new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList("$$ele", "(")), 1)), ")")), 0)))))
				.put("pages", new Json("$map", new Json("input", "$pages").put("as", "ele").put("in", new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList(new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList("$$ele", "(")), 1)), ")")), 0)))))
		));


		pipeline.addAll(getParentPipeline(grouper, remove));


		pipeline.addAll(RatingsTools.getRatingPipeline(grouper));


		pipeline.addAll(UsersAggregator.getUserPipeline(grouper, false));
		pipeline.add(Aggregates.lookup("Users", "last.user", "_id", "last.user"));
		pipeline.add(Aggregates.unwind("$last.user", new UnwindOptions().preserveNullAndEmptyArrays(true)));


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("text", new Json("$reduce", new Json("input", new Json("$split", Arrays.asList("$text", "\n"))).put("initialValue", "").put("in", new Json("$concat", Arrays.asList(
						"$$value", new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$$value", "")), " ", "")), "$$this"))))
				)
				.put("last",
						new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$last.id", new BsonUndefined())),
								new Json()
										.put("id", "$last.id")
										.put("date", "$last.date")
										.put("user", new Json("id", "$last.user.id").put("name", "$last.user.name").put("count", "$last.user.count").put("avatar",
												new Json("$concat", Arrays.asList(Settings.getCDNHttp(),
														new Json("$cond",
																Arrays.asList(new Json("$eq", Arrays.asList("$last.user.avatar", new BsonUndefined())),
																		Settings.UI_LOGO,
																		new Json("$concat", Arrays.asList("/files/", "$last.user.avatar"))))
												))

										))
								,
								new Json("date", "$last.date")
						))
				)
		));

		pipeline.add(Aggregates.project(grouper.getProjection()
						.put("text", new Json("$cond", Arrays.asList(
								new Json("$ne", Arrays.asList("$text", null)),
								new Json("$substrCP", Arrays.asList("$text", 0, new Json("$min", Arrays.asList(255, new Json("$strLenCP", "$text")))))
								, null))
						)
				)
		);

		pipeline.add(Aggregates.project(grouper.getProjection()
				.remove("forums")
				.put("breadcrumb", new Json()
						.put("id", true)
						.put("title", true)
						.put("lng", true)
						.put("domain", true)
						.put("url", true)))
		);

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));
		pipeline.add(paginer.getLastSort());

		return pipeline;
	}

	private static List<Bson> getThreadPostsPipeline(Users user, Bson filter, Paginer paginer, boolean remove) {

		Aggregator grouper = new Aggregator("id", "user", "title", "text", "coins", "docs", "changes", "comments", "remove", "link", "links", "sysid", "date", "update");

		List<Bson> pipeline = new ArrayList<>();
		List<Bson> filters = new ArrayList<>();

		if (filter != null) {
			filters.add(filter);
		}
		if (!remove) {
			filters.add(Filters.exists("remove", false));
		}
		if (paginer.getFilters() != null) {
			filters.add(paginer.getFilters());
		}

		if (filters.size() > 0) {
			pipeline.add(Aggregates.match(Filters.and(filters)));
		}

		pipeline.add(paginer.getFirstSort());

		pipeline.add(paginer.getLimit());


		pipeline.addAll(UsersAggregator.getUserPipeline(grouper, false));


		pipeline.add(Aggregates.project(grouper.getProjection()

				.put("link",
						new Json("$cond", Arrays.asList(
								new Json("$ne", Arrays.asList("$link.url", new BsonUndefined())),
								new Json().put("title", "$link.title").put("description", "$link.description").put("image",
										new Json("$concat", Arrays.asList(Settings.getCDNHttp(),
												new Json("$cond",
														Arrays.asList(new Json("$eq", Arrays.asList("$link.image", new BsonUndefined())),
																null,
																new Json("$concat", Arrays.asList("/files/", "$link.image"))))
										))
								).put("url", "$link.url")
								, null))

				)
				.put("coins",
						new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$coins", new BsonUndefined())), new Json("$size", "$coins"), 0))

				)
				.put("changes", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$changes", new BsonUndefined())), 0, new Json("$size", "$changes"))))
		));

		pipeline.addAll(BlobsDb.getBlobsPipeline(grouper));
		pipeline.addAll(DbTagsLinker.getPipeline("text", grouper));


		pipeline.addAll(getCommentsPipeline(grouper, remove));

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		pipeline.add(paginer.getLastSort());

		return pipeline;
	}

	public static List<Bson> getParentPipeline(Aggregator grouper, boolean remove) {

		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.graphLookup("Forums", new Json("$arrayElemAt", Arrays.asList("$forums", 0)), "parents.0", "_id", "breadcrumb", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$breadcrumb", new UnwindOptions().preserveNullAndEmptyArrays(true)));


		pipeline.add(Aggregates.graphLookup("Forums", "$breadcrumb._id", "parents.0", "_id", "breadcrumb.parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$breadcrumb.parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.sort(new Json("breadcrumb.parents.depth", -1)));

		pipeline.add(Aggregates.group(new Json("breadcrumb__id", "$breadcrumb._id").put("_id", "$_id"), grouper.getGrouper(
				Accumulators.first("position", "$position"),
				Accumulators.first("depth", "$depth"),
				Accumulators.first("breadcrumb_url", new Json("$arrayElemAt", Arrays.asList("$breadcrumb.url", 0))),
				Accumulators.push("urls_", new Json("$arrayElemAt", Arrays.asList("$breadcrumb.parents.url", 0))))));


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("depth", "$breadcrumb.depth")
				.put("breadcrumb_url", true)
				.put("breadcrumb", new Json()
						.put("id", "$breadcrumb._id")
						.put("title", "$breadcrumb.title")
						.put("lng", "$breadcrumb.lng")
						.put("domain",
								new Json("$arrayElemAt", Arrays.asList(
										new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList("$breadcrumb.lng", "$$domains.key"))))
										, 0))
						)
						.put("url", new Json("$reduce", new Json("input", "$urls_").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))))
						.put("order", "$breadcrumb.order")
				)

		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("depth", true)
				.put("breadcrumb_url", true)
				.put("breadcrumb", new Json()
						.put("id", "$breadcrumb.id")
						.put("title", "$breadcrumb.title")
						.put("lng", "$breadcrumb.lng")
						.put("domain", "$breadcrumb.domain.value")
						.put("url", "$breadcrumb.url")
						.put("order", "$breadcrumb.order")
				)

		));
		pipeline.add(Aggregates.sort(new Json("depth", -1)));


		pipeline.add(Aggregates.group("$_id._id", grouper.getGrouper(
				Accumulators.first("url", new Json("$arrayElemAt", Arrays.asList("$url", 0))),
				Accumulators.push("breadcrumb", "$breadcrumb"),
				Accumulators.push("urls", "$breadcrumb_url"))
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("urls", new Json("$filter", new Json("input", "$urls").put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$type", "$$ele"), "string"))))))
		);

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("index", new Json("$or", Arrays.asList(
						new Json("$eq", Arrays.asList("$index", true)),
						new Json("$gt", Arrays.asList("$replies", 0))
				)))
				.put("link",
						new Json("$cond", Arrays.asList(
								new Json("$ne", Arrays.asList("$link.url", new BsonUndefined())),
								new Json().put("title", "$link.title").put("description", "$link.description").put("image",
										new Json("$concat", Arrays.asList(Settings.getCDNHttp(),
												new Json("$cond",
														Arrays.asList(new Json("$eq", Arrays.asList("$link.image", new BsonUndefined())),
																null,
																new Json("$concat", Arrays.asList("/files/", "$link.image"))))
										))
								).put("url", "$link.url")
								, null))

				)
				.put("url", new Json("$concat", Arrays.asList(
						new Json("$cond", Arrays.asList(
								new Json("$eq", Arrays.asList(new Json("$size", "$urls"), 0)),
								"/threads",
								new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this"))))

						)),
						new Json("$cond", Arrays.asList(
								new Json("$or", Arrays.asList(
										new Json("$eq", Arrays.asList("$index", true)),
										new Json("$gt", Arrays.asList("$replies", 0))
								)), "/", "/noreply/")),
						new Json("$cond", Arrays.asList(
								new Json("$or", Arrays.asList(
										new Json("$eq", Arrays.asList("$thread_id", null)),
										new Json("$eq", Arrays.asList("$thread_id", new BsonUndefined()))
								))
								, "$_id", new Json("$concat", Arrays.asList("$thread_id", "?post=", "$_id"))

						))))
				)
		));

		/////


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("breadcrumb",
						new Json("$filter", new Json("input", "$breadcrumb").put("as", "breadcrumb").put("cond", new Json("$ne", Arrays.asList("$$breadcrumb.id", new BsonUndefined()))))
				)
		));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("lng", new Json("$arrayElemAt", Arrays.asList("$breadcrumb.lng", new Json("$subtract", Arrays.asList(new Json("$size", "$breadcrumb"), 1)))))
				.put("domain", new Json("$arrayElemAt", Arrays.asList("$breadcrumb.domain", new Json("$subtract", Arrays.asList(new Json("$size", "$breadcrumb"), 1)))))
		));

		return pipeline;
	}

	private static List<Bson> getCommentsPipeline(Aggregator grouper, boolean remove) {
		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.unwind("$comments", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("order_comment")));

		pipeline.add(Aggregates.lookup("Users", "comments.user", "_id", "comments.user"));
		pipeline.add(Aggregates.unwind("$comments.user", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("comments",
						new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$comments", new BsonUndefined())), null,
								new Json()
										.put("user", new Json("id", "$comments.user.id").put("name", "$comments.user.name").put("count", "$comments.user.count").put("avatar",
												new Json("$concat", Arrays.asList(Settings.getCDNHttp(),
														new Json("$cond",
																Arrays.asList(new Json("$eq", Arrays.asList("$comments.user.avatar", new BsonUndefined())),
																		Settings.UI_LOGO,
																		new Json("$concat", Arrays.asList("/files/", "$comments.user.avatar"))))
												))

										))
										.put("text", "$comments.text").put("date", "$comments.date").put("index", "$order_comment").put("remove", "$comments.remove")
										.put("changes", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$comments.changes", new BsonUndefined())), 0, new Json("$size", "$comments.changes"))))
						))
				))
		);

		pipeline.add(Aggregates.sort(Sorts.ascending("order_comment")));

		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(Accumulators.push("comments", "$comments"))));


		pipeline.add(Aggregates.project(grouper.getProjection()
						.put("comments",
								new Json("$cond",
										Arrays.asList(new Json("$eq", Arrays.asList(new Json("$arrayElemAt", Arrays.asList("$comments.date", 0)), new BsonUndefined())), new ArrayList<>(), "$comments")
								))
				)
		);

		if (!remove) {
			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("comments",
							new Json("$filter", new Json("input", "$comments").put("as", "posts_comments").put("cond", new Json("$eq", Arrays.asList("$$posts_comments.remove", new BsonUndefined()))))

					)));
		}
		return pipeline;
	}

	public static Json getPostsAdmin(String paging_str, String sort_str, int limit) {

		Paginer paginer = new Paginer(paging_str, sort_str, limit);

		List<Bson> pipeline = new ArrayList<>();
		pipeline.addAll(getThreadPostsPipeline(null, null, paginer, false));

		return paginer.getResult("Posts", pipeline);
	}

	public static Json getPosts(Bson filter, String paging_str, Users user, boolean remove) {

		Paginer paginer = new Paginer(paging_str, "date", number_posts);

		return paginer.getResult("Posts", getThreadPostsPipeline(user, filter, paginer, remove));
	}

	public static Json getUserPosts(Users user, String user_id, String paging_str) {

		Paginer paginer = new Paginer(paging_str, "-date", 10);

		List<Bson> filters = new ArrayList<>();

		Bson paging = paginer.getFilters();
		if (paging != null) {
			filters.add(paging);
		}
		filters.add(Filters.eq("user", user_id));
		filters.add(Filters.exists("remove", false));


		return paginer.getResult("Posts", new ArrayList<>(getThreadPostsPipeline(user, Filters.and(filters), paginer, false)));
	}

	public static Json getSimplePost(String id) {

		Aggregator grouper = new Aggregator("date", "update", "replies", "user", "parents", "forums", "roots", "breadcrumb", "url", "lng", "domain", "title", "text", "docs", "link", "links", "links", "roots");
		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.match(Filters.eq("_id", id)));
		pipeline.add(Aggregates.limit(1));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("forums", new Json("$filter", new Json("input", "$parents").put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$substrCP", Arrays.asList("$$ele", 0, "Forums(".length())), "Forums(")))))
		));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("forums", new Json("$map", new Json("input", "$forums").put("as", "ele").put("in", new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList(new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList("$$ele", "(")), 1)), ")")), 0)))))
		));

		pipeline.add(Aggregates.graphLookup("Forums", "$forums", "parents", "_id", "roots", new GraphLookupOptions().maxDepth(1000)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("roots", new Json("$map", new Json("input", "$roots").put("as", "ele").put("in", new Json("$concat", Arrays.asList("Forums(", "$$ele._id", ")")))))
		));

		pipeline.addAll(ForumsAggregator.getBreadCrumbPipeline(grouper));

		pipeline.addAll(BlobsDb.getBlobsPipeline(grouper));

		pipeline.add(Aggregates.graphLookup("Forums", new Json("$arrayElemAt", Arrays.asList("$forums", 0)), "parents.0", "_id", "urls_parents", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$urls_parents", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(new Json("urls_parents.depth", -1)));

		pipeline.add(Aggregates.group("$_id",
				grouper.getGrouper(
						Accumulators.push("urls", new Json("$arrayElemAt", Arrays.asList("$urls_parents.url", 0)))
				)
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("urls", new Json("$filter", new Json("input", "$urls").put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$type", "$$ele"), "string"))))))
		);

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("title", new Json("$cond",
						Arrays.asList(
								new Json("$or", Arrays.asList(
										new Json("$eq", Arrays.asList("$title", new BsonUndefined())),
										new Json("$eq", Arrays.asList("$title", null)),
										new Json("$eq", Arrays.asList("$title", ""))
								)), null, "$title"
						))
				).put("url",
						new Json("$concat", Arrays.asList(
								new Json("$cond", Arrays.asList(
										new Json("$eq", Arrays.asList(new Json("$size", "$urls"), 0)),
										"/threads",
										new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this"))))

								)),
								new Json("$cond", Arrays.asList(
										new Json("$or", Arrays.asList(
												new Json("$eq", Arrays.asList("$index", true)),
												new Json("$gt", Arrays.asList("$replies", 0))
										)), "/", "/noreply/")),
								"$_id"))
				)
				.put("domain",
						new Json("$arrayElemAt", Arrays.asList(
								new Json("$filter", new Json("input", Settings.LANGS_DOMAINS.toList()).put("as", "domains").put("cond", new Json("$eq", Arrays.asList("$lng", "$$domains.key"))))
								, 0))
				)
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("domain", "$domain.value")
		));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("link", new Json()
						.put("title", "$link.title")
						.put("url", "$link.url")
						.put("description", "$link.description")
						.put("logo", "$link.image")
						.put("image",
								new Json("$cond", Arrays.asList(
										new Json("$or", Arrays.asList(
												new Json("$eq", Arrays.asList("$link.image", null)),
												new Json("$eq", Arrays.asList("$link.image", new BsonUndefined()))
										)),
										null,
										new Json("$concat", Arrays.asList(Settings.getCDNHttp(), "/files/", "$link.image"))
								))
						)
				)
		));

		pipeline.add(Aggregates.project(grouper.getProjection().put("link",
				new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$link.url", new BsonUndefined())), null, "$link"))
		)));

		pipeline.addAll(UsersAggregator.getUserPipeline(grouper, false));

		pipeline.addAll(DbTagsLinker.getPipeline("text", grouper));


		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("breadcrumb",
						new Json("$filter", new Json("input", "$breadcrumb").put("as", "breadcrumb").put("cond", new Json("$ne", Arrays.asList("$$breadcrumb.id", new BsonUndefined()))))
				)
		));


		pipeline.add(Aggregates.graphLookup("Forums", "$forums", "parents", "_id", "roots", new GraphLookupOptions().maxDepth(1000)));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("roots", new Json("$map", new Json("input", "$roots").put("as", "ele").put("in", new Json("$concat", Arrays.asList("Forums(", "$$ele._id", ")")))))
		));
		pipeline.add(Aggregates.project(grouper.getProjectionOrder().remove("forums")));

		return Db.aggregate("Posts", pipeline).first();
	}


	public static List<Json> getSitemapThreads(Date date, String lng, int limit) {

		Aggregator grouper = new Aggregator("date", "update", "forums", "url", "lng", "domain", "replies", "index");
		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.match(
				Filters.and(
						Filters.or(Filters.eq("index", true), Filters.gt("replies", 0)),
						Filters.regex("parents", Pattern.compile("^Forums\\([0-9a-z]+\\)", Pattern.CASE_INSENSITIVE)),
						Filters.eq("lng", lng),
						Filters.gte("date", date))));
		pipeline.add(Aggregates.sort(Sorts.ascending("date")));

		pipeline.add(Aggregates.limit(limit));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("forums", new Json("$map", new Json("input",
						new Json("$filter", new Json("input", "$parents").put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$substrCP", Arrays.asList("$$ele", 0, "Forums(".length())), "Forums("))))
				).put("as", "ele").put("in", new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList(new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList("$$ele", "(")), 1)), ")")), 0)))))
		));

		pipeline.add(Aggregates.graphLookup("Forums", new Json("$arrayElemAt", Arrays.asList("$forums", 0)), "parents.0", "_id", "urls", new GraphLookupOptions().depthField("depth").maxDepth(50)));

		pipeline.add(Aggregates.unwind("$urls", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.sort(new Json("urls.depth", -1)));

		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(
				Accumulators.push("urls", new Json("$arrayElemAt", Arrays.asList("$urls.url", 0))))));


		pipeline.add(Aggregates.project(grouper.getProjection()
						.put("domain",
								new Json("$arrayElemAt", Arrays.asList(
										new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList("$lng", "$$domains.key"))))
										, 0))
						)
						.put("urls", true)
				)

		);

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("domain", "$domain.value")
				.put("url", new Json("$concat", Arrays.asList(
						new Json("$reduce", new Json("input", "$urls").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this"))))
						, "/", "$_id")
				))
		));

		pipeline.add(Aggregates.sort(Sorts.descending("update")));
		return Db.aggregate("Posts", pipeline).into(new ArrayList<>());
	}

	public static class PostsPipeliner extends PipelinerStore.Pipeliner {

		public PostsPipeliner(String type, String lng, Paginer paginer) {
			super(type, paginer);
			addFilter(Filters.and(Filters.eq("lng", lng), Filters.exists("remove", false)));
		}

		@Override
		protected List<Bson> getSearchPipeline() {

			List<Bson> pipeline = new ArrayList<>();

			Aggregator grouper = new Aggregator("title", "intro", "user", "parents", "thread", "score", "photo", "remove", "date", "thread_id", "post_id", "link", "replies", "lng", "domain", "url", "breadcrumb");


			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("text", new Json("$cond", Arrays.asList(
							new Json("$or", Arrays.asList(new Json("$eq", Arrays.asList("$text", new BsonUndefined())), new Json("$eq", Arrays.asList("$text", null)))),
							"$link.description"
							, "$text"))
					))
			);
			pipeline.add(Aggregates.project(grouper.getProjection()
							.put("intro", new Json("$cond", Arrays.asList(
									new Json("$or", Arrays.asList(new Json("$eq", Arrays.asList("$text", new BsonUndefined())), new Json("$eq", Arrays.asList("$text", null)))),
									null,
									new Json("$substrCP", Arrays.asList("$text", 0, new Json("$min", Arrays.asList(255, new Json("$strLenCP", "$text")))))))
							)
							.put("post_id", "$_id")
							.put("thread", new Json("$filter", new Json("input", "$parents").put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$substrCP", Arrays.asList("$$ele", 0, "Posts(".length())), "Posts(")))))
					)
			);

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("thread", new Json("$map", new Json("input", "$thread").put("as", "ele").put("in", new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList(new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList("$$ele", "(")), 1)), ")")), 0)))))
			));

			pipeline.add(Aggregates.lookup("Posts", "thread", "_id", "thread"));
			pipeline.add(Aggregates.unwind("$thread", new UnwindOptions().preserveNullAndEmptyArrays(true)));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("parents", new Json("$cond", Arrays.asList(
							new Json("$ne", Arrays.asList("$thread._id", new BsonUndefined())), "$thread.parents", "$parents"))
					)
					.put("thread_id", new Json("$cond", Arrays.asList(
							new Json("$ne", Arrays.asList("$thread._id", new BsonUndefined())), "$thread._id", null))
					)
			));


			pipeline.add(Aggregates.project(grouper.getProjection()
							.put("forums", new Json("$filter", new Json("input", "$parents").put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$substrCP", Arrays.asList("$$ele", 0, "Forums(".length())), "Forums(")))))
					)
			);

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("forums", new Json("$map", new Json("input", "$forums").put("as", "ele").put("in", new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList(new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList("$$ele", "(")), 1)), ")")), 0)))))
			));

			pipeline.addAll(getParentPipeline(grouper, false));

			pipeline.add(Aggregates.lookup("Users", "user", "_id", "user"));
			pipeline.add(Aggregates.unwind("$user", new UnwindOptions().preserveNullAndEmptyArrays(true)));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("title", new Json("$cond", Arrays.asList(
							new Json("$or", Arrays.asList(new Json("$eq", Arrays.asList("$title", null)), new Json("$eq", Arrays.asList("$title", new BsonUndefined())))),
							"$thread.title", "$title")))
					.put("url", new Json("$concat", Arrays.asList("$url", "#", "$post_id")))
					.put("text", new Json("$reduce", new Json("input", new Json("$split", Arrays.asList("$text", "\n"))).put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$$value", "")), " ", "")), "$$this")))))
					.put("breadcrumb", new Json()
							.put("id", true)
							.put("title", true)
							.put("lng", true)
							.put("domain", true)
							.put("url", true)
					)
					.put("logo", new Json("$concat", Arrays.asList(Settings.getCDNHttp(),
							new Json("$cond",
									Arrays.asList(new Json("$eq", Arrays.asList("$user.avatar", new BsonUndefined())),
											Settings.UI_LOGO,
											new Json("$concat", Arrays.asList("/files/", "$user.avatar"))))
							))
					)


					.put("svg", new Json("$cond", Arrays.asList(
							new Json("$eq", Arrays.asList("$thread_id", "$id"))
							, SVGTemplate.get("fa_icon_comment_o"), SVGTemplate.get("fa_icon_comments_o")

					)))
					.put("tag", new Json("$concat", Arrays.asList("Posts(", "$_id", ")")))
			));

			return pipeline;
		}

		@Override
		public List<Bson> getUrlDbTags(Aggregator grouper, String key) {
			List<Bson> pipeline = new ArrayList<>();

			pipeline.add(Aggregates.project(grouper.getProjection()
							.put("threadlink", new Json("$filter", new Json("input", "$" + key + ".parents").put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$substrCP", Arrays.asList("$$ele", 0, "Posts(".length())), "Posts(")))))
					)
			);

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("threadlink", new Json("$map", new Json("input", "$threadlink").put("as", "ele").put("in", new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList(new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList("$$ele", "(")), 1)), ")")), 0)))))
			));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("threadlink", new Json("$arrayElemAt", Arrays.asList("$threadlink", 0)))
			));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("threadlink", new Json("$cond", Arrays.asList(
							new Json("$ne", Arrays.asList("$threadlink", new BsonUndefined())), "$threadlink", "$" + key + "._id"))
					)

			));

			pipeline.add(Aggregates.lookup("Posts", "threadlink", "_id", "threadlink"));
			pipeline.add(Aggregates.unwind("$threadlink", new UnwindOptions().preserveNullAndEmptyArrays(true)));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("forumslink", new Json("$cond", Arrays.asList(
							new Json("$ne", Arrays.asList("$threadlink._id", new BsonUndefined())), "$threadlink.parents", "$" + key + ".parents"))
					)
					.put("threadlink", true)

			));


			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("threadlink", true)
					.put("forumslink", new Json("$filter", new Json("input", "$forumslink").put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$substrCP", Arrays.asList("$$ele", 0, "Forums(".length())), "Forums(")))))

			));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("threadlink", true)
					.put("forumslink", new Json("$map", new Json("input", "$forumslink").put("as", "ele").put("in", new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList(new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList("$$ele", "(")), 1)), ")")), 0)))))

			));


			////

			pipeline.add(Aggregates.graphLookup("Forums", new Json("$arrayElemAt", Arrays.asList("$forumslink", 0)), "parents.0", "_id", "breadcrumblink", new GraphLookupOptions().depthField("depth").maxDepth(50)));
			pipeline.add(Aggregates.unwind("$breadcrumblink", new UnwindOptions().preserveNullAndEmptyArrays(true)));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("threadlink", true)
					.put("urlslink", new Json("$arrayElemAt", Arrays.asList("$breadcrumblink.url", 0)))
					.put("depthlink", "$breadcrumblink.depth")

			));

			pipeline.add(new Json("$sort", new Json("depthlink", -1)));


			pipeline.add(Aggregates.group(new Json("_id", "$_id").put(key, "$" + key + "._id"),
					grouper.getGrouper(
							Accumulators.first("threadlink", "$threadlink"),
							Accumulators.push("urlslink", "$urlslink")
					)
			));

			pipeline.add(Aggregates.project(grouper.getProjection()
					.put("_id", "$_id._id")
					.put(key, new Json()
							.put("_id", true)
							.put("top_title", true)
							.put("title", new Json("$cond", Arrays.asList(
									new Json("$or", Arrays.asList(
											new Json("$eq", Arrays.asList("$" + key + ".title", null)),
											new Json("$eq", Arrays.asList("$" + key + ".title", new BsonUndefined()))
									))
									, "$threadlink.title", "$" + key + ".title"

							)))
							.put("url", new Json("$concat", Arrays.asList(
									new Json("$cond", Arrays.asList(
											new Json("$eq", Arrays.asList(new Json("$size", "$urlslink"), 0)),
											"/threads",
											new Json("$reduce", new Json("input", "$urlslink").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this"))))

									)),
									new Json("$cond", Arrays.asList(
											new Json("$or", Arrays.asList(
													new Json("$eq", Arrays.asList("$threadlink.index", true)),
													new Json("$gt", Arrays.asList("$threadlink.replies", 0))
											)), "/", "/noreply/")),
									new Json("$cond", Arrays.asList(
											new Json("$eq", Arrays.asList("$threadlink._id", "$" + key + "._id"))
											, "$threadlink._id", new Json("$concat", Arrays.asList("$threadlink._id", "?post=", "$" + key + "._id", "#", "$" + key + "._id"))

									))))
							)
							.put("domain", getDomainFilter("$threadlink.lng"))


					)
			));


			return pipeline;
		}
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

}
