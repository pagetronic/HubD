/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.admin;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UnwindOptions;
import live.page.hubd.content.notices.Notifier;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.json.Json;
import live.page.hubd.utils.Fx;

public class WebPushAdmin {


    public static Json push(Json data) {

        Json rez = new Json();
        String tag = Fx.getUnique();

        Pipeline pipeline = new Pipeline();

        pipeline.add(Aggregates.match(Filters.eq("lng", data.getString("lng"))));
        pipeline.add(Aggregates.group(new Json("_id", "$_id")
                        .put("config", new Json("endpoint", "$config.endpoint").put("key", "$config.key").put("auth", "$config.auth")
                                .put("user", "$user")),
                Accumulators.first("obj", "$obj"),
                Accumulators.first("config", "$config"),
                Accumulators.first("user", "$user")
        ));

        pipeline.add(Aggregates.lookup("Users", "user", "_id", "user"));
        pipeline.add(Aggregates.unwind("$user", new UnwindOptions().preserveNullAndEmptyArrays(true)));
        pipeline.add(Aggregates.project(new Json("_id", "$_id._id").put("obj", "$obj").put("user", "$user._id").put("config", "$config").put("lng", "$user.lng")));


        MongoCursor<Json> follows = Db.aggregate("Follows", pipeline).iterator();
        int count = 0;
        while (follows.hasNext()) {
            Json follow = follows.next();
            Notifier.notifyNow(follow, data.getString("title"), data.getString("text"), data.getString("url"), data.getString("lng"));
            count++;
        }
        follows.close();

        return rez.put("ok", true).put("count", count);


    }


}
