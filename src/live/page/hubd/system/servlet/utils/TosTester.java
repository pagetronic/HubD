/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.hubd.system.servlet.utils;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.system.utils.Fx;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
Routine qui vérifie les mises à jour des TOS // TODO à revoir.
 */
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
