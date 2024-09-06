/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Subscriptions {

    public static Json subscribe(Users user, String channel, String type) {
        if (channel == null) {
            return new Json("error", "INVALID_DATA");
        }
        Bson filter = Filters.and(Filters.eq("channel", channel), Filters.eq("user", user.getId()));
        return switch (type) {
            case "os", "app" -> {
                Db.updateOne("Subscriptions",
                        filter,
                        new Json()
                                .put("$set",
                                        new Json()
                                                .put("type", type)
                                )
                                .put("$setOnInsert", new Json()
                                        .put("channel", channel)
                                        .put("user", user.getId())
                                        .put("date", new Date())
                                        .put("_id", Db.getKey())
                                ),
                        new UpdateOptions().upsert(true)
                );
                yield new Json("ok", true);
            }
            case "off" -> {
                Db.deleteOne("Subscriptions", filter);
                yield new Json("ok", true);
            }
            default -> new Json("error", "INVALID_DATA");
        };
    }


    public static Json control(Users user, String channel) {
        if (channel == null) {
            return new Json("error", "INVALID_DATA");
        }
        Json subscription = Db.find("Subscriptions", Filters.and(Filters.eq("channel", channel), Filters.eq("user", user.getId()))).first();
        return new Json("type", subscription != null ? subscription.getString("type", "off") : "off");
    }


    public static Json listSubscriptions(Users user, String paging) {

        Aggregator grouper = new Aggregator("date", "channel");

        Paginer paginer = new Paginer(paging, "-date", 100);
        Pipeline pipeline = new Pipeline();

        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("user", user.getId()));
        if (paginer.getFilters() != null) {
            filters.add(paginer.getFilters());
        }
        pipeline.add(Aggregates.match(Filters.and(filters)));

        pipeline.add(paginer.getFirstSort());
        pipeline.add(paginer.getLimit());

        pipeline.add(Aggregates.project(grouper.getProjection().put("_id", false)));

        pipeline.add(paginer.getLastSort());

        return paginer.getResult("Subscriptions", pipeline);
    }

}
