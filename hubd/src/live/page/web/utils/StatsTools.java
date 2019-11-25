/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.utils;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import live.page.web.db.Db;
import live.page.web.socket.SessionData;
import live.page.web.socket.SocketMessage;
import live.page.web.utils.json.Json;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * TODO make stats public
 * TODO make live Executor service
 */
public class StatsTools {

	/**
	 * Get users on stats
	 *
	 * @return count of users
	 */
	private static int getLive() {
		List<Bson> pipeline = new ArrayList<>();

		Date date = new Date(System.currentTimeMillis() - 10 * 1000);
		Date gone = new Date(System.currentTimeMillis() - 5 * 60 * 1000);

		pipeline.add(Aggregates.match(Filters.and(
				Filters.eq("gone", null),
				Filters.gt("date", gone),
				Filters.or(
						Filters.gt("alive", date),
						Filters.gt("date", date)
				)
		)));

		pipeline.add(Aggregates.group(new Json("ip", "$ip").put("ua", "$ua")));
		pipeline.add(Aggregates.group(null, Accumulators.sum("unique", 1)));
		Json unique = Db.aggregate("Stats", pipeline).first();
		if (unique == null) {
			return 0;
		}
		return unique.getInteger("unique", 0);
	}

	/**
	 * Get users on stats and send into socket request
	 *
	 * @return socket message
	 */
	public static SocketMessage getLiveSocket(Json msg, SessionData sessiondata) {
		String act = msg.getString("act");
		SocketMessage socketMessage = new SocketMessage(act);
		final int[] live = {getLive()};
		socketMessage.putKeyMessage("live", getLive());
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(() -> {
			if ( sessiondata.isAbort(act) || !sessiondata.isOpen()) {
				executor.shutdown();
				return;
			}
			int live_new = getLive();
			if (live[0] != live_new) {
				live[0] = live_new;
				socketMessage.putKeyMessage("live", live_new);
				sessiondata.send(socketMessage);
			}
		}, 3, 3, TimeUnit.SECONDS);

		return socketMessage;
	}

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
		cl.add(Calendar.HOUR_OF_DAY, -24);
		start_date = cl.getTime();

		cl.add(Calendar.HOUR_OF_DAY, 24);
		stop_date = cl.getTime();
		stats.add(getStats(start_date, stop_date));


		cl = Calendar.getInstance(tz);
		cl.set(Calendar.HOUR_OF_DAY, 0);
		cl.set(Calendar.MINUTE, 0);
		cl.set(Calendar.SECOND, 0);


		//TODO more stats
		//Yesterday
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) - 1);
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

		//This month
		cl = Calendar.getInstance(tz);
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) - 31);
		start_date = cl.getTime();
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) + 31);
		stop_date = cl.getTime();
		stats.add(getStats(start_date, stop_date));

		//Last month
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) - 62);
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
