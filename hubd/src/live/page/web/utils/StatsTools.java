/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.utils;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import live.page.web.db.Db;
import live.page.web.utils.json.Json;
import org.bson.conversions.Bson;

import java.util.*;


/**
 * TODO make stats public
 * TODO make live Executor service
 */
public class StatsTools {

	/**
	 * Get all interested stats
	 *
	 * @return list for templating
	 */
	public static List<Json> getSimplesStats(TimeZone tz) {

		List<Json> stats = new ArrayList<>();

		Calendar cl = Calendar.getInstance(tz);

		//NOW


		Date stop_date = cl.getTime();
		cl.add(Calendar.SECOND, -10);
		Date start_date = cl.getTime();

		Json unique = Db.aggregate("Stats", Arrays.asList(
				Aggregates.match(
						Filters.or(
								Filters.and(
										Filters.eq("gone", null),
										Filters.gte("alive", start_date), Filters.lt("alive", stop_date)
								),
								Filters.and(Filters.gte("date", start_date), Filters.lt("date", stop_date))
						)
				),
				Aggregates.group(new Json("ip", "$ip").put("ua", "$ua")),
				Aggregates.group(null, Accumulators.sum("unique", 1))

		)).first();

		stats.add(new Json("unique", unique == null ? 0 : unique.getInteger("unique", 0)));


		cl = Calendar.getInstance(tz);
		//Today
		cl.set(Calendar.HOUR, 0);
		cl.set(Calendar.MINUTE, 0);
		cl.set(Calendar.SECOND, 0);
		start_date = cl.getTime();
		cl.set(Calendar.HOUR_OF_DAY, 24);
		stop_date = cl.getTime();
		stats.add(getStats(start_date, stop_date));


		//Yesterday
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) - 2);
		start_date = cl.getTime();
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) + 1);
		stop_date = cl.getTime();
		stats.add(getStats(start_date, stop_date));

		//Last week
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) - 7);
		start_date = cl.getTime();
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) + 7);
		stop_date = cl.getTime();
		stats.add(getStats(start_date, stop_date));

		//Last month
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) - 31);
		start_date = cl.getTime();
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) + 31);
		stop_date = cl.getTime();
		stats.add(getStats(start_date, stop_date));

		//Last year
		stats.add(getStats(null, null));
		return stats;
	}


	/**
	 * Get period interested stats
	 *
	 * @param start_date from date, null for all
	 * @param stop_date  to date
	 * @return view and unique client
	 */
	//TODO concat in only one query with $lookup
	//TODO view url / domain etc...
	private static Json getStats(Date start_date, Date stop_date) {
		Json rez = new Json("start", start_date).put("stop", stop_date);
		List<Bson> pipeline = new ArrayList<>();

		if (start_date != null) {
			pipeline.add(Aggregates.match(
					Filters.and(Filters.gte("date", start_date), Filters.lt("date", stop_date))
			));
		}
		pipeline.add(Aggregates.group(new Json("ip", "$ip").put("ua", "$ua")));
		pipeline.add(Aggregates.group(null, Accumulators.sum("unique", 1)));
		Json unique = Db.aggregate("Stats", pipeline).first();
		rez.put("unique", unique == null ? 0 : unique.getInteger("unique", 0));

		pipeline.clear();
		if (start_date != null) {
			pipeline.add(Aggregates.match(Filters.and(Filters.gte("date", start_date), Filters.lt("date", stop_date))));
		}
		pipeline.add(Aggregates.group(null, Accumulators.sum("view", 1)));
		Json view = Db.aggregate("Stats", pipeline).first();
		rez.put("view", view == null ? 0 : view.getInteger("view", 0));

		return rez;
	}

}
