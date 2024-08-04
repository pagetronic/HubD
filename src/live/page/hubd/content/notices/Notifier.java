/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.lang.Nullable;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.socket.SocketPusher;
import live.page.hubd.system.utils.Fx;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Notifier {


    public static void notifyNow(Json config, String title, String message, String url, String lng) {
        notify(config, null, title, message, null, url, lng, null, 0, TimeUnit.SECONDS);
    }


    public static void notify(List<String> objs, List<String> excludes, String title, String message, String url, String lng) {
        notify(objs, excludes, title, message, url, lng, 3, TimeUnit.MINUTES);
    }

    public static void notify(List<String> objs, List<String> excludes, String title, String message, String url, String lng, int pushdelay, TimeUnit pushunit) {
        String grouper = Fx.getUnique();
        List<Bson> filters = new ArrayList<>();
        if (excludes != null && excludes.size() > 0) {
            filters.add(Filters.nin("user", excludes));
        }
        filters.add(Filters.in("obj", objs));

        MongoCursor<Json> follows = Db.find("Follows", Filters.and(filters)).iterator();
        while (follows.hasNext()) {
            Json follow = follows.next();
            notify(follow.getJson("config"), follow.getString("user"), title, message, grouper, url, lng, follow.getString("obj"), pushdelay, pushunit);
        }
        follows.close();
    }


    public static void notify(String user_id, String title, String message, String url, String lng, String tag) {

        String grouper = Fx.getUnique();
        MongoCursor<Json> follows = Db.aggregate("Follows", Arrays.asList(
                Aggregates.match(Filters.eq("user", user_id)),
                Aggregates.group(new Json().put("config", new Json("endpoint", "$config.endpoint").put("key", "$config.key").put("auth", "$config.auth")),
                        Accumulators.first("config", "$config")
                ),
                Aggregates.project(new Json("_id", false).put("config", true))
        )).iterator();
        while (follows.hasNext()) {
            notify(follows.next().getJson("config"), user_id, title, message, grouper, url, lng, tag, 3, TimeUnit.MINUTES);
        }
        follows.close();
    }

    private static void notify(@Nullable Json config, @Nullable String user_id, String title, String message, String grouper, String url, String lng, @Nullable String tag, int pushdelay, TimeUnit pushunit) {

        if (tag == null) {
            tag = Fx.getUnique();
        }

        Date date = new Date();

        Json notice = new Json();

        if (user_id != null) {
            notice.put("user", user_id);
        }
        notice.put("title", title);
        notice.put("message", message);
        if (grouper != null) {
            notice.put("grouper", grouper);
        }
        notice.put("url", url);
        notice.put("lng", lng);
        notice.put("tag", tag);
        notice.put("date", date);

        if (config != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.SECOND, (int) pushunit.toSeconds(pushdelay));
            notice.put("delay", cal.getTime());
            notice.put("config", config);
        }

        Db.save("Notices", notice);

        if (user_id != null) {
            SocketPusher.sendNoticesCount(user_id);
        }

    }

}
