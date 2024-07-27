/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.users;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class RelationsUtils {

    public static void addRelation(String from, String with, boolean permanent) {
        if (from.equals(with)) {
            return;
        }

        List<String> users = Arrays.asList(from, with);
        Collections.sort(users);
        Date date = new Date();
        if (permanent) {
            Db.updateOne("Relations",
                    Filters.eq("users", users),
                    new Json("$set", new Json("relations", true)).put("$setOnInsert", new Json("_id", Db.getKey()).put("users", users)),
                    new UpdateOptions().upsert(true));
        } else {
            try {
                Db.updateOne("Relations",
                        Filters.eq("users", users),
                        new Json("$push", new Json("relations", date)).put("$setOnInsert", new Json("_id", Db.getKey()).put("users", users)),
                        new UpdateOptions().upsert(true));
            } catch (Exception e) {
            }
        }

    }

    public static void removeRelation(String from, String with) {
        List<String> users = Arrays.asList(from, with);
        Collections.sort(users);
        Db.deleteOne("Relations", Filters.eq("users", users));
    }

    public static Json search(String search, Object filter, String paging, Users user) {


        Paginer paginer = new Paginer(paging, "-count", 20);
        Bson paging_filter = paginer.getFilters();


        Pipeline pipeline = new Pipeline();

        pipeline.add(Aggregates.match(Filters.eq("users", user.getId())));

        pipeline.add(Aggregates.unwind("$users"));

        pipeline.add(Aggregates.match(Filters.ne("users", user.getId())));

        pipeline.add(Aggregates.group("$users", List.of(
                Accumulators.first("count",
                        new Json("$cond",
                                Arrays.asList(
                                        new Json("$eq", Arrays.asList("$relations", true)),
                                        Integer.MAX_VALUE, new Json("$size", "$relations"))
                        )
                )
        )));


        pipeline.add(new Json("$lookup", new Json("from", "Users").put("as", "user").put("let", new Json("id", "$_id"))
                .put("pipeline",
                        Arrays.asList(
                                Aggregates.match(
                                        Filters.and(
                                                Filters.expr(new Json("$eq", Arrays.asList("$_id", "$$id"))),
                                                Filters.or(
                                                        Filters.regex("name", Pattern.compile(search, Pattern.CASE_INSENSITIVE)),
                                                        Filters.eq("_id", search)
                                                )
                                        )
                                ),
                                Aggregates.limit(1),
                                Aggregates.project(new Json("_id", false).put("name", true))
                        )
                )
        ));

        pipeline.add(Aggregates.unwind("$user"));
        pipeline.add(Aggregates.project(new Json().put("name", "$user.name").put("count", true)));


        if (filter != null) {
            pipeline.add(Aggregates.match(Filters.nin("_id", filter)));
        }
        if (paging_filter != null) {
            pipeline.add(Aggregates.match(paging_filter));
        }

        pipeline.add(paginer.getFirstSort());
        pipeline.add(paginer.getLimit());
        pipeline.add(paginer.getLastSort());


        return paginer.getResult("Relations", pipeline);
    }


}
