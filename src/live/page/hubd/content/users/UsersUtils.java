/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.users;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UnwindOptions;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.system.sessions.UsersBase;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class UsersUtils {
    public static int postCount(String user_id, int inc) {
        Json user = Db.findOneAndUpdate("Users", Filters.eq("_id", user_id), new Json("$inc", new Json("posts", inc)));
        return (user == null) ? 0 : user.getInteger("posts", 0);
    }

    public static Json searchChildren(Json data, Users user) {
        return search(data, Filters.eq("parent", user.getId()));

    }

    private static Json search(Json data, Bson filterer) {

        Paginer paginer = new Paginer(data.getString("paging"), "-join", 20);


        Pipeline pipeline = new Pipeline();
        List<Bson> filters = new ArrayList<>();
        if (filterer != null) {
            filters.add(filterer);
        }

        if (!data.getString("search", "").isEmpty()) {
            filters.add(
                    Filters.or(
                            Filters.eq("_id", data.getString("search", "")),
                            Filters.regex("name", Pattern.compile(data.getString("search"), Pattern.CASE_INSENSITIVE))
                    )
            );
        }
        if (data.containsKey("filter") && !data.getList("filter").isEmpty()) {
            filters.add(Filters.nin("_id", data.getList("filter")));
        }

        if (paginer.getFilters() != null) {
            filters.add(paginer.getFilters());
        }
        if (!filters.isEmpty()) {
            pipeline.add(Aggregates.match(Filters.and(filters)));
        }

        pipeline.add(paginer.getFirstSort());

        pipeline.add(paginer.getLimit());

        pipeline.add(Aggregates.lookup("BlobFiles", "avatar", "_id", "avatar"));
        pipeline.add(Aggregates.unwind("$avatar", new UnwindOptions().preserveNullAndEmptyArrays(true)));

        pipeline.add(paginer.getLastSort());

        pipeline.add(Aggregates.project(new Json()
                .put("id", "$_id")
                .put("name", "$name")
                .put("avatar",
                        new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$avatar._id", new BsonUndefined())),
                                new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$avatar._id")),
                                "$$REMOVE"
                        ))
                )));

        return paginer.getResult("Users", pipeline);
    }

    public static Json create(Json data, Users user) {


        Json children = UsersBase.getBase();

        if (data.getString("name", "").isEmpty() || data.getString("name", "").isEmpty()) {
            return new Json("error", "DATA_REQUIRED");
        }
        children.put("name", uniqueName(data.getString("name"))).put("parent", user.getId());
        if (data.containsKey("avatar")) {
            children.put("avatar", data.getString("avatar"));
        }
        Db.save("Users", children);
        return children;
    }


    public static String uniqueName(String name) {
        int num = 1;
        if (UsersUtils.existName(name)) {
            while (UsersUtils.existName(name + " (" + num + ")")) {
                num++;
            }
            name = name + " (" + num + ")";
        }
        return name;
    }

    public static String uniqueName(String user_id, String name) {
        if (existName(user_id, name)) {
            return uniqueName(name);
        }
        return name;
    }

    public static boolean existName(String name) {
        return Db.exists("Users", Filters.regex("name", Pattern.compile("^" + Pattern.quote(name) + "$", Pattern.CASE_INSENSITIVE)));
    }

    public static boolean existName(String user_id, String name) {
        return Db.exists("Users", Filters.and(Filters.ne("_id", user_id), Filters.regex("name", Pattern.compile("^" + Pattern.quote(name) + "$", Pattern.CASE_INSENSITIVE))));

    }
}
