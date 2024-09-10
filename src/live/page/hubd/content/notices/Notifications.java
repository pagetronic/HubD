/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import live.page.hubd.system.db.Db;
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
public class Notifications implements ServletContextListener {

    private static final ExecutorService service = Executors.newFixedThreadPool(5);

    public static void notifyUser(String user_id, String title, String message, String url, String icon) {
        Db.find("Devices", Filters.eq("user", user_id))
                .forEach(device -> save(user_id, title, message, url, icon, "user", device.getId(), "test"));

        SocketPusher.sendNoticesCount(user_id);

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
            String grouper = Db.getKey();
            MongoCursor<Json> subscriptions = Db.find("Subscriptions", Filters.and(filters)).sort(Sorts.ascending("date")).iterator();
            while (subscriptions.hasNext()) {
                Json subscription = subscriptions.next();
                save(subscription.getString("user"), title, message, url, icon,
                        subscription.getString("channel"), subscription.getString("device"), grouper);
            }
            subscriptions.close();
        });
    }


    private static void save(String user_id, String title, String message, String url, String icon, String channel, String device, String grouper) {

        if (channel == null) {
            channel = Fx.getUnique();
        }

        Json notice = new Json();

        if (user_id != null) {
            notice.put("user", user_id);
        }
        notice.put("title", title);
        notice.put("message", message);
        notice.put("device", device);

        notice.put("url", url);
        notice.put("channel", channel);
        notice.put("date", new Date());
        notice.put("icon", icon);
        if (grouper != null) {
            notice.put("grouper", grouper);
        }
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
