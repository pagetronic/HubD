/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.socket.SocketPusher;
import live.page.hubd.system.utils.Fx;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@WebListener
public class NoticesSender implements ServletContextListener {

    private static final ExecutorService service = Executors.newFixedThreadPool(5);

    public static void notifyUser(String user_id, String title, String message, String url, String icon) {

        service.submit(() -> {
            Pipeline pipeline = new Pipeline();
            if (user_id != null) {
                pipeline.add(Aggregates.match(Filters.eq("user", user_id)));
            }
            pipeline.add(Aggregates.group("$user", List.of(
                    Accumulators.push("devices", "$_id")
            )));
            MongoCursor<Json> devices = Db.aggregate("Devices", pipeline).iterator();
            while (devices.hasNext() && !service.isShutdown() && !service.isTerminated()) {
                Json device = devices.next();
                save(device.getId(), title, message, url, icon, "user", List.of());
                SocketPusher.sendNoticesCount(user_id);
            }
            devices.close();
        });
    }

    public static void notify(String channel, String exclude, String title, String message, String url, String icon) {
        notify(channel, exclude, title, message, url);
    }

    public static void notify(String channel, String exclude, String title, String message, String url) {
        notify(channel, exclude == null ? List.of() : List.of(exclude), title, message, url, null);
    }

    public static void notify(String channel, List<String> excludes, String title, String message, String url, String icon) {
        service.submit(() -> {
            List<Bson> filters = new ArrayList<>();
            if (excludes != null && !excludes.isEmpty()) {
                filters.add(Filters.nin("user", excludes));
            }
            filters.add(Filters.eq("channel", channel));

            MongoCursor<Json> subscriptions = Db.aggregate("Subs",
                    List.of(
                            Aggregates.match(Filters.and(filters)),
                            Aggregates.group("$user",
                                    List.of(
                                            Accumulators.push("subs", "$_id")
                                    )),
                            Aggregates.sort(Sorts.ascending("date")),
                            Aggregates.limit(100)
                    )


            ).iterator();
            while (subscriptions.hasNext() && !service.isShutdown() && !service.isTerminated()) {
                Json subscription = subscriptions.next();
                save(subscription.getId(), title, message, url, icon, channel, subscription.getList("subs"));
            }
            subscriptions.close();
        });
    }


    private static void save(String user_id, String title, String message, String url, String icon, String channel, List<String> subs) {

        if (channel == null) {
            channel = Fx.getUnique();
        }

        Json notice = new Json();

        if (user_id != null) {
            notice.put("user", user_id);
        }
        notice.put("title", title);
        notice.put("message", message);
        if (subs.isEmpty()) {
            notice.put("subs", subs);
        }
        notice.put("url", url);
        notice.put("channel", channel);
        notice.put("date", new Date());
        notice.put("icon", icon);

        Db.save("Notices", notice);

        if (user_id != null) {
            SocketPusher.sendNoticesCount(user_id);
        }

    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Fx.shutdownService(service);
    }
}
