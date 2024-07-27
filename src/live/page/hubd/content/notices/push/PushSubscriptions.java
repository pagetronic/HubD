/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices.push;

import com.mongodb.client.model.*;
import live.page.hubd.content.notices.Notifier;
import live.page.hubd.content.users.UsersAggregator;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import org.bson.conversions.Bson;

import java.util.Date;

public class PushSubscriptions {

    public static Json subscribe(String user_id, String lng, Json device, Json config, String obj) {
        if (config == null || obj == null) {
            return new Json("ok", false);
        }
        device.sort();
        config.sort();
        Json followdb = Db.findOneAndUpdate("Follows",
                Filters.and(Filters.eq("obj", obj), Filters.eq("config", config)),
                new Json()
                        .put("$set", new Json().put("obj", obj).put("config", config).put("device", device).put("user", user_id).put("lng", lng))
                        .put("$setOnInsert", new Json()
                                .put("date", new Date())
                                .put("_id", Db.getKey())
                        ),
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
        );
        return new Json("ok", followdb != null);
    }

    public static Json unsubscribe(Json config, String obj) {
        if (config == null || obj == null) {
            return new Json("ok", false);
        }
        config.sort();
        return new Json("ok", Db.deleteOne("Follows", Filters.and(Filters.eq("obj", obj), Filters.eq("config", config))));

    }

    public static Json control(Json config, String obj) {
        config.sort();
        return new Json("follow", Db.exists("Follows", Filters.and(Filters.eq("obj", obj), Filters.eq("config", config))));
    }


    public static Json listConfigFollows(Json config, String paging) {
        config.sort();
        return listFollows(Filters.eq("config", config), paging);
    }

    public static Json listUserFollows(Users user, String paging) {
        return listFollows(Filters.eq("user", user.getId()), paging);
    }

    private static Json listFollows(Bson filter, String paging) {

        Aggregator grouper = new Aggregator("id", "date", "user", "update", "obj", "device", "config");

        Paginer paginer = new Paginer(paging, "-date", 100);
        Pipeline pipeline = new Pipeline();

        pipeline.add(Aggregates.match(paginer.getFilters() != null ? Filters.and(paginer.getFilters(), filter) : filter));


        pipeline.add(paginer.getFirstSort());
        pipeline.add(paginer.getLimit());
        pipeline.add(Aggregates.sort(Sorts.descending("date")));

        pipeline.add(Aggregates.group(new Json("config", "$config").put("device", "$device").put("user", "$user"), grouper.getGrouper(
                Accumulators.first("id", "$_id"),
                Accumulators.first("update", "$date"),
                Accumulators.last("date", "$date"),
                Accumulators.push("obj", "$obj")
        )));
        pipeline.addAll(UsersAggregator.getUserPipeline(grouper, "user", false));

        pipeline.add(Aggregates.project(grouper.getProjection().put("_id", false)));
        pipeline.add(Aggregates.project(grouper.getProjectionOrder().remove("config")));

        pipeline.add(paginer.getLastSort());


        return paginer.getResult("Follows", pipeline);
    }

    public static Json remove(String id, Json config, Users user) {
        if (config != null) {
            config.sort();
        }
        if (user == null) {
            long deleted = Db.deleteMany("Follows", Filters.eq("config", config)).getDeletedCount();
            return new Json("ok", deleted > 0).put("deleted", deleted);
        }

        Json follow = Db.find("Follows", Filters.or(
                Filters.and(
                        Filters.eq("config", config),
                        Filters.eq("_id", id)
                ),
                Filters.and(
                        Filters.eq("user", user.getId()),
                        Filters.eq("_id", id)
                ))
        ).first();

        if (follow != null) {
            long deleted = Db.deleteMany("Follows", Filters.and(
                    Filters.eq("config", follow.getJson("config")),
                    Filters.eq("user", follow.getString("user"))
            )).getDeletedCount();

            return new Json("ok", deleted > 0).put("deleted", deleted);
        }
        return new Json("ok", false);
    }

    public static Json test(Json config, String lng) {
        Notifier.notifyNow(config, "Push Test", "Test Push", Settings.getFullHttp(lng), lng);
        return new Json("ok", true);
    }
}
