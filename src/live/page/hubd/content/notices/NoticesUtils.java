/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.system.socket.SocketMessage;
import live.page.hubd.system.socket.SocketPusher;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
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

    public static Json getNotices(Users user, String next_str) {


        Aggregator grouper = new Aggregator("tag", "title", "message", "elements", "tag", "url", "date", "read", "icon");
        Paginer paginer = new Paginer(next_str, "-date", 10);

        Pipeline pipeline = new Pipeline();
        List<Bson> filters = new ArrayList<>();

        filters.add(Filters.eq("user", user.getId()));
        Bson paging = paginer.getFilters();
        if (paging != null) {
            filters.add(paging);
        }

        pipeline.add(Aggregates.match(Filters.and(filters)));

        pipeline.add(Aggregates.group(new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$grouper", new BsonUndefined())), "$_id", "$grouper")), grouper.getGrouper(
                Accumulators.first("id", "$_id")
        )));

        pipeline.add(paginer.getFirstSort());
        pipeline.add(paginer.getLimit());

        pipeline.add(Aggregates.project(grouper.getProjection().put("_id", "$id").put("url", new Json("$concat", Arrays.asList("/notices/", "$id")))));
        pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

        pipeline.add(paginer.getLastSort());

        Json notices = paginer.getResult("Notices", pipeline);
        List<String> ids = new ArrayList<>();
        notices.getListJson("result").forEach((Json result) -> {
            if (!result.getBoolean("received", false)) {
                ids.add(result.getId());
            }
        });

        Db.updateMany("Notices", Filters.in("_id", ids), new Json("$set", new Json("received", true)));
        return notices;

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
        json.put("unread", SocketPusher.countUnreads(user_id));
        return json;
    }

    public static void setRead(String tag, String user_id) {
        if (Db.updateOne("Notices", Filters.and(Filters.eq("tag", tag), Filters.eq("user", user_id)), new Json("$set", new Json("read", new Date()))).getModifiedCount() > 0) {
            SocketPusher.sendNoticesCount(user_id);
        }
    }

    public static Json remove(String user_id, Json data) {
        Json json = new Json("ok", false);
        if (data.getId() != null) {
            json.put("ok", Db.deleteOne("Notices", Filters.and(Filters.eq("user", user_id), Filters.eq("_id", data.getId()))));
        } else if (data.getList("ids") != null) {
            json.put("ok", true);
            json.put("count", Db.deleteMany("Notices", Filters.and(Filters.eq("user", user_id), Filters.in("_id", data.getList("ids")))).getDeletedCount());
        }
        return json;
    }

    public static SocketMessage noticeReceived(String user_id) {
        // todo Webpush read, mettre l'id du webpush  (1)
        Db.updateMany("Notices", Filters.and(Filters.eq("user", user_id), Filters.ne("received", true)), new Json("$set", new Json("received", true)));
        return new SocketMessage();
    }


}
