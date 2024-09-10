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
import live.page.hubd.system.socket.SocketPusher;
import live.page.hubd.system.utils.Fx;
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

    public static Json getNotices(Users user, String start, String device, String next_str) {


        Aggregator grouper = new Aggregator("count", "follow", "title", "message", "icon", "type", "channel", "url", "date", "read", "read", "received");
        Paginer paginer = new Paginer(next_str, "-date", device != null ? 40 : 10);

        Pipeline pipeline = new Pipeline();
        List<Bson> filters = new ArrayList<>();

        filters.add(Filters.eq("user", user.getId()));
        if (start != null) {
            try {
                Date date = Fx.dateFormater.parse(start);
                filters.add(Filters.gt("date", date));
            } catch (Exception e) {
                Fx.log("Date parse error");
            }
        }
        if (device != null) {
            filters.add(Filters.eq("device", device));
            filters.add(Filters.ne("received", true));
        }

        Bson paging = paginer.getFilters();
        if (paging != null) {
            filters.add(paging);
        }

        pipeline.add(Aggregates.match(Filters.and(filters)));
        pipeline.add(paginer.getLastSort());

        pipeline.add(Aggregates.group(
                new Json("$cond", new Json()
                        .put("if", new Json("$eq", Arrays.asList("$grouper", new BsonUndefined())))
                        .put("then", "$_id")
                        .put("else", "$grouper")
                ),
                grouper.getGrouper(
                        Accumulators.first("id", "$_id")
                )));
        pipeline.add(paginer.getLastSort());

        pipeline.add(Aggregates.group(
                "$channel",
                grouper.getGrouper(
                        Accumulators.first("id", "$id"),
                        Accumulators.sum("count", 1)
                )));

        pipeline.add(paginer.getFirstSort());

        pipeline.add(paginer.getLimit());

        pipeline.add(paginer.getLastSort());

        pipeline.add(Aggregates.lookup("Subscriptions", "channel", "channel", "follow"));

        pipeline.add(Aggregates.project(grouper.getProjection()
                .put("_id", "$id")
                .put("follow",
                        new Json("$cond", new Json()
                                .put("if", new Json("$gt", Arrays.asList(new Json("$size", "$follow"), 0)))
                                .put("then", true)
                                .put("else", false)
                        )
                )));
        pipeline.add(Aggregates.project(grouper.getProjectionOrder().prepend("_id", "$_id")));


        Json notices = paginer.getResult("Notices", pipeline);

        List<Json> result = notices.getListJson("result");
        if (device == null && !result.isEmpty()) {
            Bson filter = Filters.and(
                    Filters.eq("user", user.getId()),
                    Filters.gte("date", result.get(result.size() - 1).getDate("date")),
                    Filters.lte("date", result.get(0).getDate("date")),
                    Filters.ne("received", true)
            );
            Db.updateMany("Notices", Filters.and(filter),
                    new Json("$set", new Json("received", true)));
        }
        SocketPusher.sendNoticesCount(user.getId());

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
        json.put("unread", NoticesUtils.countNotices(user_id));
        return json;
    }

    public static Json remove(String user_id, Json data) {
        Json rez = new Json("ok", false);
        if (data.getId() != null) {
            rez.put("ok", Db.deleteMany("Notices", Filters.and(Filters.eq("user", user_id),
                    Filters.or(
                            Filters.eq("_id", data.getId()),
                            Filters.eq("grouper", data.getId()),
                            Filters.eq("channel", data.getId())
                    )
            )));
        } else if (data.getList("ids") != null) {
            rez.put("ok", true);
            rez.put("count", Db.deleteMany("Notices", Filters.and(Filters.eq("user", user_id),
                    Filters.or(
                            Filters.in("_id", data.getList("ids")),
                            Filters.in("grouper", data.getList("ids")),
                            Filters.in("channel", data.getList("ids"))
                    )
            )).getDeletedCount());
        }
        return rez;
    }

    public static String countNotices(String user_id) {
        List<Json> notices = Db.aggregate("Notices", List.of(
                Aggregates.match(Filters.and(Filters.eq("user", user_id), Filters.exists("read", false))),
                Aggregates.project(new Json().put("grouper", true)),
                Aggregates.group(new Json()
                        .put("if", new Json("$eq", Arrays.asList("$grouper", new BsonUndefined())))
                        .put("then", "$_id")
                        .put("else", "$grouper")
                ),
                Aggregates.limit(100)
        )).into(new ArrayList<>());
        int counts = notices.size();
        return counts >= 100 ? "99+" : counts + "";
    }


}
