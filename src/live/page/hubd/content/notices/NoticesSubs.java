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

public class NoticesSubs {

    public static Json subscribe(Users user, String channel, String type, String device) {
        if (channel == null) {
            return new Json("error", "INVALID_DATA");
        }
        Bson filterGlobal = Filters.and(Filters.eq("channel", channel), Filters.eq("user", user.getId()));
        Bson filterDevice = Filters.and(filterGlobal,
                Filters.or(
                        Filters.eq("device", device),
                        Filters.eq("device", null)
                )
        );
        switch (type) {
            case "off": {
                Db.deleteMany("Subs", filterGlobal);
                break;
            }
            case "app": {
                Db.updateOne("Subs",
                        filterDevice,
                        new Json()
                                .put("$set", new Json("type", "app"))
                                .put("$unset", new Json("device", ""))
                                .put("$setOnInsert", new Json()
                                        .put("channel", channel)
                                        .put("user", user.getId())
                                        .put("date", new Date())
                                        .put("_id", Db.getKey())
                                ),
                        new UpdateOptions().upsert(true)
                );
                break;
            }
            case "os": {
                Db.updateOne("Subs",
                        filterDevice,
                        new Json()
                                .put("$set", new Json("type", "os").put("device", device))
                                .put("$setOnInsert", new Json()
                                        .put("channel", channel)
                                        .put("user", user.getId())
                                        .put("date", new Date())
                                        .put("_id", Db.getKey())
                                ),
                        new UpdateOptions().upsert(true)
                );
                break;
            }
            default:
                return new Json("error", "INVALID_DATA");

        }
        return new Json("ok", true);

    }


    public static Json control(Users user, String channel, String device) {
        if (channel == null) {
            return new Json("error", "INVALID_DATA");
        }

        List<Json> subscriptions = Db.find("Subs", Filters.and(
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
