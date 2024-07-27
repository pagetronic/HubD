/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.users;

import com.mongodb.client.model.*;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.system.sessions.UsersBase;
import org.bson.BsonNull;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class UsersAggregator {

    public static Json getUsers(List<String> ids) {
        if (ids.size() > 1000) {
            ids.subList(0, 1000);
        }

        Aggregator grouper = UsersBase.getGrouper();
        Pipeline pipeline = new Pipeline();

        pipeline.add(Aggregates.match(Filters.in("_id", ids)));

        pipeline.add(Aggregates.lookup("Groups", "groups", "_id", "groups"));
        pipeline.add(Aggregates.lookup("BlobFiles", "avatar", "_id", "avatar"));
        pipeline.add(Aggregates.unwind("$avatar", new UnwindOptions().preserveNullAndEmptyArrays(true)));

        pipeline.add(Aggregates.project(grouper.getProjection()
                .put("posts", new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$posts", new BsonUndefined())), "$posts", 0)))
                .put("groups", new Json()
                        .put("_id", true)
                        .put("name", true)
                        .put("color", true)
                )
                .put("avatar", new Json("$cond",
                        Arrays.asList(new Json("$eq", Arrays.asList("$avatar._id", new BsonUndefined())),
                                null,
                                new Json("$concat", Arrays.asList(Settings.getCDNHttp(), "/files/", "$avatar._id"))))


                )
        ));


        return new Json("result", Db.aggregate("Users", pipeline).into(new ArrayList<>()));
    }

    public static Json getUsers(String paging_str, String query) {


        Aggregator grouper = UsersBase.getGrouper();

        Paginer paginer = new Paginer(paging_str, "-coins", 30);

        Pipeline pipeline = new Pipeline();
        List<Bson> filters = new ArrayList<>();

        Bson paging = paginer.getFilters();
        if (paging != null) {
            filters.add(paging);
        }
        if (query != null) {
            filters.add(
                    Filters.or(
                            Filters.regex("name", Pattern.compile(query, Pattern.CASE_INSENSITIVE)),
                            Filters.regex("email", Pattern.compile(query, Pattern.CASE_INSENSITIVE))
                    )
            );
        }

        if (!filters.isEmpty()) {
            pipeline.add(Aggregates.match(Filters.and(filters)));
        }
        pipeline.add(paginer.getFirstSort());
        pipeline.add(paginer.getLimit());


        pipeline.add(Aggregates.lookup("Groups", "groups", "_id", "groups"));
        pipeline.add(Aggregates.lookup("BlobFiles", "avatar", "_id", "avatar"));
        pipeline.add(Aggregates.unwind("$avatar", new UnwindOptions().preserveNullAndEmptyArrays(true)));

        pipeline.add(Aggregates.project(grouper.getProjection()
                .put("posts", new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$posts", new BsonUndefined())), "$posts", 0)))
                .put("groups", new Json()
                        .put("_id", true)
                        .put("name", true)
                        .put("color", true)
                )
                .put("avatar", new Json("$cond",
                        Arrays.asList(new Json("$eq", Arrays.asList("$avatar._id", new BsonUndefined())),
                                null,
                                new Json("$concat", Arrays.asList(Settings.getCDNHttp(), "/files/", "$avatar._id"))))


                ).put("email", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$email", query)), "$email", null)))
        ));

        pipeline.add(paginer.getLastSort());


        return paginer.getResult("Users", pipeline);

    }

    public static Json getUserData(String user_id) {

        Pipeline pipeline = new Pipeline();
        pipeline.add(Aggregates.match(Filters.eq("_id", user_id)));
        pipeline.add(Aggregates.lookup("Groups", "groups", "_id", "groups"));
        pipeline.add(Aggregates.project(new Json()
                        .put("name", true)
                        .put("join", true)
                        .put("coins", true)
                        .put("locale", true)
                        .put("posts", new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$posts", new BsonUndefined())), "$posts", 0)))
                        .put("avatar",
                                new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$avatar", new BsonUndefined())),
                                        new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$avatar")),
                                        Settings.getCDNHttp() + Settings.UI_LOGO
                                ))
                        )
                        .put("last", true)
                        .put("groups", new Json()
                                .put("_id", true)
                                .put("name", true)
                                .put("color", true)
                        )
                )
        );

        return Db.aggregate("Users", pipeline).first();
    }

    public static List<Bson> getUserPipeline(Aggregator grouper, String key, boolean multiple) {

        Pipeline pipeline = new Pipeline();

        pipeline.add(Aggregates.addFields(new Field<>(key + "_save", "$" + key)));

        pipeline.add(Aggregates.lookup("Users", key, "_id", key));

        pipeline.add(Aggregates.unwind("$" + key, new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("users_position")));
        pipeline.add(Aggregates.sort(Sorts.ascending("users_position")));
        pipeline.add(Aggregates.project(grouper.getProjection()
                .put(key + "_save", "$" + key + "_save")
                .put(key,
                        new Json("id", "$" + key + "._id")
                                .put("name", "$" + key + ".name").put("url", new Json("$concat", Arrays.asList("/users/", "$" + key + "._id")))
                                .put("avatar",
                                        new Json("$cond",
                                                Arrays.asList(new Json("$eq", Arrays.asList("$" + key + ".avatar", new BsonUndefined())),
                                                        null,
                                                        new Json("$concat", Arrays.asList(Settings.getCDNHttp(), "/files/", "$" + key + ".avatar"))))

                                )
                )
        ));
        pipeline.add(Aggregates.sort(Sorts.ascending("users_position")));
        pipeline.add(Aggregates.group("$_id", grouper.getGrouper(
                Accumulators.first(key + "_save", "$" + key + "_save"),
                multiple ? Accumulators.push(key, "$" + key) : Accumulators.first(key, "$" + key)
        )));
        if (multiple) {
            pipeline.add(Aggregates.project(grouper.getProjection()
                            .put(key + "_save", "$" + key + "_save")
                            .put(key, new Json("$filter", new Json("input", "$" + key).put("as", key).put("cond", new Json("$ne", Arrays.asList("$$" + key + ".id", new BsonUndefined())))))
                    )
            );
        }

        pipeline.add(Aggregates.project(grouper.getProjection()
                .put(key, new Json("$cond", Arrays.asList(
                        new Json("$or", List.of(
                                new Json("$eq", Arrays.asList("$" + key + "_save", new BsonNull())),
                                new Json("$eq", Arrays.asList("$" + key + "_save", new BsonUndefined()))
                        )),
                        null,
                        "$" + key
                )))));

        return pipeline;
    }

    public static Json simpleUser(Users user) {
        if (user == null) {
            return null;
        }
        return new Json("id", user.getId())
                .put("name", user.getString("name"))
                .put("posts", user.getInteger("posts", 0))
                .put("url", user.getString("url"))
                .put("avatar", user.getString("avatar"));
    }
}
