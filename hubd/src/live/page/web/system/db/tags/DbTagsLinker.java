/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.system.db.tags;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.UnwindOptions;
import live.page.web.system.Settings;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.db.PipelinerStore;
import live.page.web.system.json.Json;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DbTagsLinker {

	/**
	 * Detect DbTags(XXX) and produce url in links key
	 *
	 * @param key     where parse tags
	 * @param grouper aggregator used in the aggregation
	 * @return a liste of operation to add to other aggregation operations
	 */
	public static List<Bson> getPipeline(String key, Aggregator grouper) {

		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.project(grouper.getProjection().put("links", new ArrayList<>())));
		Map<String, Class<? extends PipelinerStore.Pipeliner>> pipeliner = PipelinerStore.getMethods();

		for (String parent : Settings.VALID_PARENTS) {

			if (pipeliner.containsKey(parent.toLowerCase())) {
				pipeline.add(Aggregates.project(grouper.getProjection().put("links" + parent, new Json("$slice", Arrays.asList(new Json("$split", Arrays.asList("$" + key, parent + "(")), 1, 100)))));


				pipeline.add(Aggregates.unwind("$links" + parent, new UnwindOptions().preserveNullAndEmptyArrays(true)));
				pipeline.add(Aggregates.project(grouper.getProjection().put("links" + parent, new Json("$substrCP", Arrays.asList("$links" + parent, 0, Db.DB_KEY_LENGTH)))));

				pipeline.add(Aggregates.group("$_id", grouper.getGrouper(Accumulators.push("links" + parent, "$links" + parent))));

				pipeline.add(Aggregates.project(grouper.getProjection()
						.put("links" + parent, new Json("$map", new Json("input", "$links" + parent).put("as", "ele").put("in", new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList("$$ele", ")")), 0)))))
				));

				pipeline.add(Aggregates.project(grouper.getProjection()
						.put("links" + parent, new Json("$filter", new Json("input", "$links" + parent).put("as", "ele").put("cond", new Json("$eq", Arrays.asList(new Json("$strLenCP", "$$ele"), Db.DB_KEY_LENGTH)))))
				));

				pipeline.add(Aggregates.unwind("$links" + parent, new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("links_order")));
				//links
				pipeline.add(Aggregates.lookup(parent, "links" + parent, "_id", "links" + parent));

				pipeline.add(Aggregates.unwind("$links" + parent, new UnwindOptions().preserveNullAndEmptyArrays(true)));

				pipeline.addAll(PipelinerStore.getConstructor(parent.toLowerCase()).getUrlDbTags(grouper.clone()
						.addKey("links_order").addKey("links" + parent), "links" + parent));

				pipeline.add(Aggregates.project(grouper.getProjection()
						.put("links_order", true)
						.put("links" + parent,
								new Json()
										.put("id", "$links" + parent + "._id")
										.put("top_title", "$links" + parent + ".top_title")
										.put("title", "$links" + parent + ".title")
										.put("url", "$links" + parent + ".url")
										.put("domain", "$links" + parent + ".domain")
										.put("tag", new Json("$concat", Arrays.asList(parent + "(", "$links" + parent + "._id", ")")))
										.put("logo", "$links.logo")
						)
				));


				pipeline.add(new Json("$sort", new Json("links_order", 1)));
				pipeline.add(Aggregates.group("$_id", grouper.getGrouper(Accumulators.push("links" + parent, "$links" + parent))));


				pipeline.add(Aggregates.project(grouper.getProjection()
						.put("links" + parent,
								new Json("$filter", new Json("input", "$links" + parent).put("as", "ele").put("cond", new Json("$ne", Arrays.asList("$$ele.id", new BsonUndefined()))))
						)
				));


				pipeline.add(Aggregates.project(grouper.getProjection().put("links_", new Json("$concatArrays", Arrays.asList("$links", "$links" + parent)))));
				pipeline.add(Aggregates.project(grouper.getProjection().put("links", "$links_")));

			}
		}


		return pipeline;
	}

	/**
	 * Make clickable DbTags
	 *
	 * @param text  where replace tags
	 * @param links to replace
	 * @return parsed string
	 */
	public static String parse(String text, List<Json> links) {
		if (links != null) {

			for (Json link : links) {
				Pattern pattern = Pattern.compile("\\[" + Pattern.quote(link.getString("tag")) + " ?([^]]+)]", Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(text);

				while (matcher.find()) {
					String title = matcher.group(1);
					if (matcher.group(1) == null || title.equals("")) {
						title = link.getString("title");
					}
					String replacement = "<a href=\"" + link.getString("url") + "\"";
					if (link.containsKey("top_title")) {
						replacement += " title=\"" + link.getString("top_title").replace("\"", "\\\"") + "\"";
					} else if (!link.getString("title", "").equals(title)) {
						replacement += " title=\"" + link.getString("title").replace("\"", "\\\"") + "\"";
					}
					text = text.replace(matcher.group(), replacement + ">" + title + "</a>");

				}
				text = pattern.matcher(text).replaceAll("$1");
			}
		}

		text = Pattern.compile("\\[(" + StringUtils.join(Settings.VALID_PARENTS, "|") + ")\\([a-zA-Z0-9]+\\) ?([^]]+)]")
				.matcher(text).replaceAll("$2");

		return text;

	}
}
