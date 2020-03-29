/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.congrate;

import com.mongodb.client.model.*;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.ServletUtils;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.sessions.Users;
import org.bson.conversions.Bson;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@WebServlet(urlPatterns = {"/rating"}, displayName = "rating")
public class RatingsTools extends HttpServlet {


	@Override
	public void doPostApiPublic(ApiServletRequest req, ApiServletResponse resp, Json data) throws IOException {
		Json rez = new Json("error", "NOT_FOUND");
		if (data.getString("action", "").equals("rate")) {
			rez = setRating(data.getString("obj"), data.getInteger("rate", -1), ServletUtils.realIp(req));
		}
		resp.sendResponse(rez);
	}

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {
		doPostApiPublic(req, resp, data);
	}

	private Json setRating(String obj, int rate, String ip) {

		String[] obj_arr = obj.split("([()])");

		if (obj_arr.length == 2 && rate >= 0 && rate <= 5) {
			if (rate == 0) {
				Db.deleteOne("Ratings",
						Filters.and(
								Filters.eq("src", obj_arr[1]),
								Filters.eq("type", obj_arr[0]),
								Filters.eq("ip", ip)
						)
				);
			} else {
				Db.deleteOne("Ratings",
						Filters.and(
								Filters.eq("src", obj_arr[1]),
								Filters.eq("type", obj_arr[0]),
								Filters.eq("ip", ip)
						));
				Db.save("Ratings", new Json("rate", rate).put("date", new Date()).put("src", obj_arr[1]).put("type", obj_arr[0]).put("ip", ip));
			}
			Aggregator grouper = new Aggregator("value", "count");
			Json rates = Db.aggregate("Ratings", Arrays.asList(
					Aggregates.match(Filters.and(
							Filters.eq("src", obj_arr[1]),
							Filters.eq("type", obj_arr[0])
					)),
					Aggregates.group(new Json("src", "$src").put("type", "$type"),
							Accumulators.avg("value", "$rate"),
							Accumulators.sum("count", 1)
					),
					Aggregates.project(grouper.getProjectionOrder().put("_id", false))
			)).first();

			if (rates != null) {
				return rates.put("ok", true);
			}
		}
		return new Json("ok", false);
	}

	public static List<Json> getRatings() {
		List<Bson> pipeline = new ArrayList<>();
		Aggregator grouper = new Aggregator("rate", "src", "date", "title", "url", "type", "src", "ip");

		pipeline.add(Aggregates.sort(Sorts.descending("date")));
		pipeline.add(Aggregates.limit(200));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("page", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$type", "Pages")), "$src", null)))
				.put("thread", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$type", "Posts")), "$src", null)))
		));
		pipeline.add(Aggregates.lookup("Posts", "thread", "_id", "thread"));

		pipeline.add(Aggregates.lookup("Pages", "page", "_id", "page"));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("page", new Json().put("title", true).put("_id", true).put("url", true).put("type", "Pages"))
				.put("thread", new Json().put("title", true).put("_id", true).put("type", "Posts"))
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("element", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$size", "$page"), 1)), "$page", "$thread")))
		));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("element", new Json("$arrayElemAt", Arrays.asList("$element", 0)))
		));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("title", "$element.title")
				.put("url", "$element.url")
				.put("type", "$element.type")
				.put("src", "$element._id")
		));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("url", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$type", "Pages")),
						new Json("$concat", Arrays.asList("/", "$url")),
						new Json("$concat", Arrays.asList("/threads/", "$src"))))
				)

		));

		pipeline.add(Aggregates.lookup("Ratings", "src", "src", "rating"));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("count", new Json("$size", "$rating"))
				.put("avg", new Json("$avg", "$rating.rate"))
		));

		return Db.aggregate("Ratings", pipeline).into(new ArrayList<>());

	}


	public static List<Bson> getRatingPipeline(Aggregator grouper) {

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.lookup("Ratings", "_id", "src", "rating"));
		pipeline.add(Aggregates.unwind("$rating", new UnwindOptions().preserveNullAndEmptyArrays(true)));

		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(
				Accumulators.avg("review_value", "$rating.rate"),
				Accumulators.sum("review_count", 1)
		)));

		pipeline.add(Aggregates.project(grouper.getProjection().put("review",
				new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$review_value", null)), new Json("count", 0),
						new Json("count", "$review_count").put("value",
								new Json("$subtract", Arrays.asList(
										new Json("$add", Arrays.asList("$review_value", 0.0049999999999999999)),
										new Json("$mod", Arrays.asList(new Json("$add", Arrays.asList("$review_value", 0.0049999999999999999)), 0.01))
								)))
				))
		)));
		return pipeline;
	}
}
