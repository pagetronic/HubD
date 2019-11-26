/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.users;

import com.mongodb.client.model.*;
import live.page.web.system.Settings;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.json.Json;
import live.page.web.system.sessions.Users;
import live.page.web.system.sessions.UsersBase;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class UsersAggregator {

	public static Json getUsers(String paging_str, String query, Users user) {


		Aggregator grouper = UsersBase.getGrouper();

		Paginer paginer = new Paginer(paging_str, "-coins", 30);

		List<Bson> pipeline = new ArrayList<>();
		List<Bson> filters = new ArrayList<>();

		Bson paging = paginer.getFilters();
		if (paging != null) {
			filters.add(paging);
		}
		if (query != null) {
			if (user != null && user.getEditor()) {
				filters.add(
						Filters.or(
								Filters.regex("name", Pattern.compile(query, Pattern.CASE_INSENSITIVE)),
								Filters.regex("providers.email", Pattern.compile(query, Pattern.CASE_INSENSITIVE))
						)
				);
			} else {
				filters.add(Filters.regex("name", Pattern.compile(query, Pattern.CASE_INSENSITIVE)));
			}
		}

		if (filters.size() > 0) {
			pipeline.add(Aggregates.match(Filters.and(filters)));
		}
		pipeline.add(paginer.getFirstSort());
		pipeline.add(paginer.getLimit());


		pipeline.add(Aggregates.lookup("Teams", "teams", "_id", "teams"));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("posts", new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$posts", new BsonUndefined())), "$posts", 0)))
				.put("teams", new Json()
						.put("_id", true)
						.put("name", true)
						.put("color", true)
				)
				.put("avatar",
						new Json("$concat", Arrays.asList(Settings.getCDNHttp(),
								new Json("$cond",
										Arrays.asList(new Json("$eq", Arrays.asList("$avatar", new BsonUndefined())),
												Settings.UI_LOGO,
												new Json("$concat", Arrays.asList("/files/", "$avatar"))))
						))

				)
		));

		pipeline.add(paginer.getLastSort());


		return paginer.getResult("Users", pipeline);

	}

	public static Json getUserData(String user_id) {

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.match(Filters.eq("_id", user_id)));
		pipeline.add(Aggregates.lookup("Teams", "teams", "_id", "teams"));
		pipeline.add(Aggregates.project(new Json()
						.put("name", true)
						.put("join", true)
						.put("coins", true)
						.put("locale", true)
						.put("posts", new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$posts", new BsonUndefined())), "$posts", 0)))
						.put("avatar",
								new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$avatar", new BsonUndefined())),
										new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$avatar")),
										Settings.getCDNHttp() + Settings.UI_LOGO
								))
						)
						.put("last", true)
						.put("teams", new Json()
								.put("_id", true)
								.put("name", true)
								.put("color", true)
						)
				)
		);

		return Db.aggregate("Users", pipeline).first();
	}

	public static List<Bson> getUserPipeline(Aggregator grouper, boolean multiple) {

		String user_field = "user" + (multiple ? "s" : "");
		List<Bson> pipeline = new ArrayList<>();
		if (multiple) {
			pipeline.add(Aggregates.unwind("$" + user_field, new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("users_position")));
		}
		pipeline.add(Aggregates.lookup("Users", user_field, "_id", user_field));
		pipeline.add(Aggregates.unwind("$" + user_field, new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.sort(Sorts.ascending("users_position")));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put(user_field,
						new Json("id", "$" + user_field + "._id")

								.put("posts", "$" + user_field + ".posts")
								.put("coins", "$" + user_field + ".coins")
								.put("name", "$" + user_field + ".name").put("url", new Json("$concat", Arrays.asList("/users/", "$" + user_field + "._id")))
								.put("avatar",
										new Json("$concat", Arrays.asList(Settings.getCDNHttp(),
												new Json("$cond",
														Arrays.asList(new Json("$eq", Arrays.asList("$" + user_field + ".avatar", new BsonUndefined())),
																Settings.UI_LOGO,
																new Json("$concat", Arrays.asList("/files/", "$" + user_field + ".avatar"))))
										))
								)
				)
		));
		if (multiple) {
			pipeline.add(Aggregates.sort(Sorts.ascending("users_position")));
			pipeline.add(Aggregates.group("$_id", grouper.getGrouper(Accumulators.push(user_field, "$" + user_field))));
		}
		return pipeline;
	}

	public static Json simpleUser(Users user) {
		if (user == null) {
			return null;
		}
		return new Json("id", user.getId())
				.put("name", user.getString("name"))
				.put("posts", user.getInteger("posts", 0))
				.put("url", user.getString("url"))
				.put("avatar", user.getString("avatar"));
	}
}
