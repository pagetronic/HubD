/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.posts.utils;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.json.Json;
import live.page.web.system.sessions.Users;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ThreadsUtils {

	public static Json search(Json data) {

		Paginer paginer = new Paginer(data.getString("paging"), "-replies", 30);
		Bson next = paginer.getFilters();

		List<Bson> pipeline = new ArrayList<>();
		List<Bson> filters = new ArrayList<>();

		filters.add(Filters.eq("remove", null));
		if (!data.getString("search", "").equals("")) {
			filters.add(Filters.regex("title", Pattern.compile(data.getString("search"), Pattern.CASE_INSENSITIVE)));
		}

		if (next != null) {
			filters.add(next);
		}
		if (data.containsKey("filter")) {
			filters.add(Filters.nin("_id", data.getList("filter")));
		}

		filters.add(Filters.ne("_id", "ROOT"));
		if (filters.size() > 0) {
			pipeline.add(Aggregates.match(Filters.and(filters)));
		}
		pipeline.add(paginer.getFirstSort());
		pipeline.add(paginer.getLimit());

		pipeline.add(Aggregates.project(new Json("_id", false).put("id", "$_id").put("title", "$title").put("date", true)));

		pipeline.add(paginer.getLastSort());

		return paginer.getResult("Posts", pipeline);
	}

	public static Json edit(String id, Users user) {

		if (user == null) {
			return new Json("error", "PLEASE_LOGIN");
		}
		Json post = ThreadsAggregator.getSimplePost(id);
		if (post == null) {
			return new Json("error", "NOT_FOUND");
		}
		if (!user.getEditor() && !user.getId().equals(post.getJson("user").getId())) {
			return new Json("error", "NOT_AUTHORIZED");
		}

		return post;

	}
}
