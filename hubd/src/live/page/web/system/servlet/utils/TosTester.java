/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.system.servlet.utils;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener
public class TosTester implements ServletContextListener {

	private static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
	private static final Map<String, Date> update = new HashMap<>();

	public static boolean isNotSeen(Users user, String lng) {
		Date date = update.get(lng);
		return (date != null && user.getDate("tos", new Date(0)).before(date));
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		service.scheduleAtFixedRate(() -> {
			try {
				MongoCursor<Json> tos = Db.find("Pages", Filters.eq("url", "tos")).iterator();
				while (tos.hasNext()) {
					Json to = tos.next();
					update.put(to.getString("lng"), to.getDate("update"));
				}
				tos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 1, 5, TimeUnit.MINUTES);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Fx.shutdownService(service);

	}
}
