/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices;

import com.mongodb.client.model.*;
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

public class NoticesView {

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
        
        Aggregator grouper = new Aggregator("ids", "count", "date", "update", "read", "title", "message", "url", "icon", "channel", "type", "devices");
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

        Bson paging = paginer.getFilters();
        if (paging != null) {
            filters.add(paging);
        }

        pipeline.add(Aggregates.match(Filters.and(filters)));
        pipeline.add(paginer.getLastSort());

        pipeline.add(Aggregates.group("$channel",
                grouper.getGrouper(
                        Accumulators.push("ids", "$_id"),
                        Accumulators.sum("count", 1),
                        Accumulators.first("subs", "$subs"),
                        Accumulators.last("date", "$date"),
                        Accumulators.last("id", "$_id"),
                        Accumulators.first("update", "$date"),
                        Accumulators.push("read", new Json("$cond", new Json()
                                        .put("if", new Json("$eq", Arrays.asList("$read", true)))
                                        .put("then", true)
                                        .put("else", false)
                                )
                        )
                )
        ));

        pipeline.add(paginer.getFirstSort());
        pipeline.add(paginer.getLimit());


        pipeline.add(Aggregates.lookup("Subs", "subs", "_id", "subs"));

        if (device != null) {
            pipeline.add(Aggregates.unwind("$subs", new UnwindOptions().preserveNullAndEmptyArrays(true)));
            pipeline.add(Aggregates.match(
                    Filters.or(
                            Filters.eq("subs.device", device),
                            Filters.eq("subs.device", new BsonUndefined())
                    )
            ));
        } else {
            pipeline.add(Aggregates.addFields(
                    new Field<>("_id", "$id"),
                    new Field<>("read", new Json("$cond",
                            new Json()
                                    .put("if", new Json("$eq", Arrays.asList(new Json("$size", "$read"), 0)))
                                    .put("then", false)
                                    .put("else", new Json("$allElementsTrue", "$read"))
                    )),
                    new Field<>("devices", new Json("$map",
                            new Json("input", "$subs")
                                    .put("as", "subs")
                                    .put("in",
                                            new Json("$cond", new Json()
                                                    .put("if", new Json("$eq", Arrays.asList("$$subs.device", new BsonUndefined())))
                                                    .put("then", "$$REMOVE")
                                                    .put("else", "$$subs.device")
                                            )


                                    )
                    )
                    )));
        }


        pipeline.add(paginer.getLastSort());
        pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

        Json notices = paginer.getResult("Notices", pipeline);

        List<Json> result = notices.getListJson("result");
        if (!result.isEmpty()) {
            List<String> ids = new ArrayList<>();
            result.forEach(json -> ids.addAll(json.getList("ids")));
            if (Db.updateMany("Notices", Filters.in("_id", ids),
                    new Json("$set", new Json("received", true))).getModifiedCount() > 0) {
                SocketPusher.sendNoticesCount(user.getId());
            }
        }


        return notices;

    }

    public static Json read(String user_id, Json data) {

        Json rez = new Json("ok", false);
        if (data.getBoolean("readAll", false)) {
            rez.put("ok", true);
            rez.put("count", Db.updateMany("Notices", Filters.and(
                            Filters.eq("user", user_id),
                            Filters.eq("read", null)
                    ),
                    new Json().put("$set", new Json("read", true))
            ));
        } else if (data.containsKey("id")) {
            rez.put("ok", Db.updateOne("Notices", Filters.and(
                            Filters.eq("user", user_id), Filters.eq("_id", data.getId())
                    ),
                    new Json().put("$set", new Json("read", true))
            ));
        } else if (data.containsKey("ids")) {
            rez.put("ok", true);
            rez.put("count", Db.updateMany("Notices", Filters.and(Filters.eq("user", user_id), Filters.in("_id", data.getList("ids"))),
                    new Json().put("$set", new Json("read", true))
            ));
        }
        return rez;
    }

    public static Json remove(String user_id, Object ids) {

        Bson filter;
        if (ids instanceof String) {
            filter = Filters.and(Filters.eq("user", user_id), Filters.eq("_id", ids));
        } else if (ids instanceof List) {
            filter = Filters.and(Filters.eq("user", user_id), Filters.in("_id", (List<String>) ids));
        } else {
            return new Json("error", "INVALID_DATA");
        }
        return new Json("ok", true).put("count", Db.deleteMany("Notices", filter).getDeletedCount());

    }

    public static String count(String user_id) {
        long counts = Db.countLimit("Notices",
                Filters.and(
                        Filters.eq("user", user_id),
                        Filters.eq("received", null)
                ),
                100);
        return counts >= 100 ? "99+" : counts + "";
    }


}
