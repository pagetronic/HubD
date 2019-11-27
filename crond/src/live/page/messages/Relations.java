/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.messages;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Relations {

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			Fx.shutdownService(scheduler);
		}));
	}


	/**
	 * Perform relationships to users who already discussed
	 * TODO: verify
	 */
	public static void cron() {

		scheduler.scheduleAtFixedRate(() -> {
			try {
				Date limit = new Date(System.currentTimeMillis() - (3600000L * 24L * 90L));
				MongoCursor<Json> relations = Db.find("Relations",
						Filters.and(Filters.ne("relations", true), Filters.lt("relations", limit))).iterator();
				while (relations.hasNext()) {
					Json relation = relations.next();
					List<Date> dates = new ArrayList<>();
					relation.getList("relations", Date.class).forEach(date -> {
						if (date.before(limit)) {
							dates.add(date);
						}
					});
					Db.updateOne("Relations", Filters.eq("_id", relation.getId()), new Json("$pull", new Json("relations", new Json("$in", dates))));
				}
				relations.close();
				Db.deleteMany("Relations", Filters.eq("relations", new ArrayList<>()));

			} catch (Exception e) {
				Fx.log(e.getMessage());
			}

		}, 30, 24 * 60, TimeUnit.MINUTES);

	}
}
