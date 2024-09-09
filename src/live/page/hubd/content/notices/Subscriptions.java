/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Subscriptions {

    public static Json subscribe(Users user, String channel, String type, String device) {
        if (channel == null) {
            return new Json("error", "INVALID_DATA");
        }
        Bson filter = Filters.and(Filters.eq("channel", channel), Filters.eq("user", user.getId()));
        if (type.isEmpty()) {
            return new Json("error", "INVALID_DATA");
        } else if (type.equals("off") || type.equals("app")) {
            Db.deleteMany("Subscriptions", filter);
        }
        if (type.equals("os") || type.equals("app")) {
            Db.updateOne("Subscriptions",
                    device != null ? Filters.and(Filters.eq("device", device)) : filter,
                    new Json()
                            .put("$set",
                                    new Json()
                                            .put("type", type)
                            )
                            .put("$setOnInsert", new Json()
                                    .put("channel", channel)
                                    .put("device", device)
                                    .put("user", user.getId())
                                    .put("date", new Date())
                                    .put("_id", Db.getKey())
                            ),
                    new UpdateOptions().upsert(true)
            );
            return new Json("ok", true);
        }

        return new Json("ok", false);
    }


    public static Json control(Users user, String channel, String device) {
        if (channel == null) {
            return new Json("error", "INVALID_DATA");
        }

        List<Json> subscriptions = Db.find("Subscriptions", Filters.and(
                Filters.eq("channel", channel),
                Filters.eq("user", user.getId())
        )).into(new ArrayList<>());

        if (subscriptions.isEmpty()) {
            return new Json("type", "off");
        }
        for (Json subscription : subscriptions) {
            if (subscription.getString("type", "").equals("os") &&
                    subscription.getString("device", "").equals(device)) {
                return new Json("type", "os");
            }
        }
        return new Json("type", "app");
    }


}
