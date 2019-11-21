/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.utils;

import live.page.web.db.Pipeliner;
import live.page.web.pages.PagesAggregator;
import live.page.web.posts.utils.ThreadsAggregator;
import live.page.web.utils.json.Json;
import live.page.web.utils.paginer.Paginer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObjsUtils {

	private static final Map<String, Class<? extends Pipeliner>> searchers = new HashMap<>();

	static {
		addPipeliner("posts", ThreadsAggregator.PostsPipeliner.class);
		addPipeliner("pages", PagesAggregator.PagesPipelines.class);
	}

	public static void addPipeliner(String key, Class<? extends Pipeliner> cls) {
		searchers.put(key, cls);
	}

	public static Map<String, Class<? extends Pipeliner>> getSearchers() {
		return searchers;
	}

	public static Pipeliner getConstructor(String key) {
		try {
			return searchers.get(key.toLowerCase()).getConstructor(String.class, String.class, Paginer.class).newInstance(null, null, null);
		} catch (Exception e) {
			return null;
		}
	}

	public static String parse(String text, List<Json> links) {
		if (links == null || links.size() == 0) {
			return text;
		}

		for (Json link : links) {
			Pattern pattern = Pattern.compile("\\[" + Pattern.quote(link.getString("tag")) + "[ ]+?([^]]+)?]", Pattern.CASE_INSENSITIVE);
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
		}
		return text;

	}
}
