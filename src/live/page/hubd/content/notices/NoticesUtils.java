/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.BaseSession;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.system.socket.SocketPusher;
import live.page.hubd.system.utils.Fx;
import org.bson.conversions.Bson;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NoticesUtils {

    public static Json readClick(String id) {

        Json notice = Db.findById("Notices", id);
        if (notice != null) {
            Db.updateMany("Notices", Filters.or(Filters.eq("_id", notice.getId()), Filters.eq("grouper", notice.getString("grouper", "00"))),
                    new Json()
                            .put("$set", new Json("read", new Date()))
                            .put("$unset", new Json("delay", ""))
            );
            if (!notice.getString("user", "").isEmpty()) {
                SocketPusher.sendNoticesCount(notice.getString("user"));
            }
        }
        return notice;
    }

    public static Json getNotices(Users user, String start, String type, String next_str) {


        Aggregator grouper = new Aggregator("title", "message", "icon", "type", "channel", "url", "date", "read", "read", "received");
        Paginer paginer = new Paginer(next_str, "-date", type != null ? 40 : 10);

        Pipeline pipeline = new Pipeline();
        List<Bson> filters = new ArrayList<>();

        filters.add(Filters.eq("user", user.getId()));
        if (start != null) {
            try {
                Date date = Fx.ISO_DATE.parse(start);
                filters.add(Filters.gt("date", date));
            } catch (ParseException e) {
                Fx.log("Date parse error");
            }
        }
        if (!"app".equals(type) && type != null) {
            filters.add(Filters.eq("type", type));
            filters.add(Filters.ne("received", true));
        }

        Bson paging = paginer.getFilters();
        if (paging != null) {
            filters.add(paging);
        }

        pipeline.add(Aggregates.match(Filters.and(filters)));
        pipeline.add(paginer.getFirstSort());

        pipeline.add(paginer.getLimit());

        pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

        pipeline.add(paginer.getLastSort());

        return paginer.getResult("Notices", pipeline);

    }

    public static Json read(String user_id, Json data) {

        Json json = new Json("ok", false);
        if (data.containsKey("readall")) {
            json.put("ok", true);
            json.put("count", Db.updateMany("Notices", Filters.and(Filters.eq("user", user_id), Filters.exists("read", false)),
                    new Json()
                            .put("$set", new Json("read", new Date()))
                            .put("$unset", new Json("delay", ""))
            ));
        } else if (data.containsKey("id")) {
            json.put("ok", Db.updateOne("Notices", Filters.and(Filters.eq("user", user_id), Filters.eq("_id", data.getId())),
                    new Json()
                            .put("$set", new Json("read", new Date()))
                            .put("$unset", new Json("delay", ""))
            ));
        } else if (data.containsKey("ids")) {
            json.put("ok", true);
            json.put("count", Db.updateMany("Notices", Filters.and(Filters.eq("user", user_id), Filters.in("_id", data.getList("ids"))),
                    new Json()
                            .put("$set", new Json("read", new Date()))
                            .put("$unset", new Json("delay", ""))
            ));
        }
        json.put("unread", BaseSession.countNotices(user_id));
        return json;
    }

    public static Json remove(String user_id, Json data) {
        Json rez = new Json("ok", false);
        if (data.getId() != null) {
            rez.put("ok", Db.deleteOne("Notices", Filters.and(Filters.eq("user", user_id), Filters.eq("_id", data.getId()))));
        } else if (data.getList("ids") != null) {
            rez.put("ok", true);
            rez.put("count", Db.deleteMany("Notices", Filters.and(Filters.eq("user", user_id), Filters.in("_id", data.getList("ids")))).getDeletedCount());
        }
        return rez;
    }


    public static Json received(String user_id, List<String> ids) {
        Db.updateMany("Notices",
                Filters.and(Filters.eq("user", user_id), Filters.in("_id", ids), Filters.ne("received", true)),
                new Json("$set", new Json("received", true)));
        SocketPusher.sendNoticesCount(user_id);
        return new Json("ok", true);
    }
}
