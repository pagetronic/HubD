/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.threads;

import com.mongodb.client.model.*;
import live.page.hubd.content.notices.NoticesSender;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.utils.Antiflood;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.system.utils.Fx;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DiscussPoster {

    public static Json post(Json data, Users user, String ip) {

        for (String key : Arrays.asList("title", "text")) {
            if (data.get(key) != null) {
                data.put(key, Fx.normalizePost(data.getText(key)));
            }
        }

        if (data.getId() == null && !Settings.getLangs().contains(data.getString("lng", Settings.getLang(data.getString("domain"))))) {
            return new Json("error", "NO_LANG");
        }

        Json ctrl = control(data, user, ip);

        if (ctrl.containsKey("errors") || ctrl.containsKey("error")) {
            return ctrl;
        }

        if (data.getString("title", "").isEmpty() && data.containsKey("page")) {
            data.put("title", Fx.truncate(data.getString("text"), 60));
        }

        if (data.getId() != null) {
            return postEdit(data, user, ip);
        }
        List<Json> parents = new ArrayList<>();
        if (data.getString("parent") != null) {
            String[] parent = data.getString("parent").split("/");
            if (parent.length == 2) {
                parents.add(new Json().put("type", parent[0]).put("id", parent[1]));

            }
        }

        return postPost(parents, data, user, ip);
    }


    private static Json control(Json data, Users user, String ip) {
        Json response = new Json();

        if (Antiflood.isFlood(user != null ? user.getId() : ip)) {
            return new Json("error", "FLOOD_ERROR").put("delay", Settings.FLOOD_DELAY / 1000);
        }

        if (data.containsKey("text")) {
            if (data.getString("title", "").isEmpty()
                    && (!data.containsKey("text") || data.getText("text").length() < (data.containsKey("page") ? 10 : 3))) {
                response.add("errors", new Json("element", "text").put("message", "TOO_SHORT"));
            } else if (data.containsKey("text") && data.getText("text").length() > 20000) {
                response.add("errors", new Json("element", "text").put("message", "TOO_LONG"));
            }
        }


        if (data.containsKey("title")) {
            if (data.getString("title").length() > 100) {
                response.add("errors", new Json("element", "title").put("message", "TOO_LONG"));
            } else if (data.getString("title").length() < 10) {
                response.add("errors", new Json("element", "title").put("message", "TOO_SHORT"));
            }
        }

        if (data.containsKey("docs")) {
            List<String> docs = data.getList("docs");
            for (String doc : docs) {
                if (!Db.exists("BlobFiles", Filters.eq("_id", doc))) {
                    response.add("errors", new Json("element", "docs").put("message", "INVALID_DOC"));
                }
            }
        }

        return response;
    }

    private static Json postPost(List<Json> parents, Json data, Users user, String ip) {
        String lng = data.getString("lng", Settings.getLang(data.getString("domain")));
        Date date = new Date();

        Json post = new Json();

        if (user != null) {
            post.put("user", user.getId());
        } else {
            post.put("sysid", data.getString("sysid"));
        }

        post.put("date", date);

        if (data.getString("title") != null) {
            post.put("title", data.getString("title"));
        }
        post.put("text", data.getText("text"));
        post.put("docs", data.getList("docs") == null ? new ArrayList<>() : data.getList("docs"));

        post.put("ip", ip);

        post.put("parents", parents);

        if (user != null) {
            post.put("user", user.getId());
        }


        boolean isReply = false;
        if (parents != null) {
            for (Json parent : parents) {
                if (parent.getString("type").equals("Posts")) {
                    isReply = true;
                }
            }
        }

        if (!isReply) {
            post.put("title", data.getString("title", Fx.truncate(data.getString("text"), 70)));
        }
        post.put("date", date).put("last", new Json("date", date)).put("update", date).put("replies", 0);

        post.put("lng", lng);


        if (Db.save("Posts", post)) {


            if (parents != null) {
                for (Json parent : parents) {
                    if (parent.getString("type").equals("Posts")) {
                        Db.updateOne(parent.getString("type"), Filters.eq("_id", parent.getId()),
                                new Json()
                                        .put("$set",
                                                new Json("last", new Json("user", post.getString("user")).put("date", post.getDate("date")).put("id", post.getId())).put("update", date)
                                        )
                                        .put("$inc", new Json("replies", 1))
                        );
                    }
                }
            }
        } else {
            return new Json("error", "INVALID_DATA");
        }
        for (Json parent : parents) {
            if (parent.getString("type").equals("Posts")) {
                Json threadParent = ThreadsAggregator.getThread(parent.getId(), user, null);
                NoticesSender.notify("posts/" + threadParent.getId(), user.getId(),
                        threadParent.getString("title", Fx.truncate(threadParent.getString("text", ""), 80)),
                        post.getString("text"),
                        threadParent.getString("url"));
            }
        }

        return ThreadsAggregator.getThread(post.getId(), user, null);

    }

    private static Json postEdit(Json data, Users user, String ip) {

        if (user == null) {
            return new Json("error", "PLEASE_LOGIN");
        }

        Date date = new Date();
        List<Bson> filter = new ArrayList<>();
        filter.add(Filters.eq("_id", data.getId()));
        if (!user.getAdmin()) {
            filter.add(Filters.eq("user", user.getId()));
        }

        Json previous = Db.find("Posts", Filters.and(filter)).first();
        if (previous == null) {
            return new Json("error", "NOT_FOUND");
        }
        Json update = new Json();

        Json set = new Json();
        Json change = new Json();
        Json thread = new Json("id", data.getId() != null ? data.getId() : previous.getString("thread"));

        if (data.containsKey("title") && !data.getString("title").equals(previous.getString("title"))) {
            change.put("title", previous.getString("title"));
            set.put("title", data.getString("title"));
            thread.put("title", data.getString("title"));
        }
        if (data.containsKey("text") && !data.getText("text").equals(previous.getText("text"))) {
            change.put("text", previous.getText("text"));
            set.put("text", data.getText("text"));
        }
        if (data.containsKey("docs") && !data.getList("docs").equals(previous.getList("docs"))) {
            change.put("docs", previous.getList("docs"));
            set.put("docs", data.getList("docs"));
        } else {
            change.put("docs", new ArrayList<>());
            set.put("docs", new ArrayList<>());
        }

        if (data.getString("parent") != null) {
            String[] parent = data.getString("parent").split("/");
            if (parent.length == 2) {
                set.add("parents", new Json().put("type", parent[0]).put("id", parent[1]));
            }
        }

        if (change.isEmpty()) {
            return new Json("ok", "NO_MODIFICATIONS");
        }


        change.put("ip", ip);

        set.put("update", date);

        change.put("user", user.getId()).put("date", date);

        update.put("$set", set);
        update.put("$push", new Json("changes", new Json("$each", List.of(change)).put("$position", 0)));

        Json new_post = Db.findOneAndUpdate("Posts", Filters.and(filter), update, new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

        if (new_post == null) {
            return new Json("error", "NOT_FOUND");
        }

        Json update_thread = new Json("update", date);
        if (thread.containsKey("title")) {
            update_thread.put("title", thread.getString("title"));
        }
        Db.updateOne("Posts", Filters.eq("_id", previous.getString("thread")), new Json("$set", update_thread));

        return new Json("ok", true).put("post", ThreadsAggregator.getThread(data.getId(), user, null));
    }


    public static Json remove(Json data, Users user) {


        if (user == null) {
            return new Json("error", "PLEASE_LOGIN");
        }

        Date date = new Date();

        Json change = new Json("user", user.getId()).put("date", date);


        if (data.getId() != null) {
            Json update = new Json();
            if (data.getBoolean("restore", false)) {
                update.put("$unset", new Json("remove", ""));
                change.put("restore", date);
            } else {
                update.put("$set", new Json("remove", date));
                change.put("remove", date);
            }
            update.put("$push", new Json("changes", new Json("$each", List.of(change)).put("$position", 0)));

            List<Bson> filter = new ArrayList<>();
            filter.add(Filters.eq("_id", data.getId()));
            if (!user.getAdmin()) {
                filter.add(Filters.eq("user", user.getId()));
            }
            Json post = Db.findOneAndUpdate("Posts", Filters.and(filter), update, new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
            if (post != null && post.getList("parents") != null) {
                for (Json parent : post.getListJson("parents")) {
                    if (parent.getString("type") == null || !parent.getString("type").equals("Posts")) {
                        continue;
                    }
                    Json update_ = new Json();
                    Json last_ = Db.find("Posts", Filters.and(Filters.eq("parents", parent.toString()), Filters.exists("remove", false))).sort(Sorts.descending("date")).first();
                    Json last = new Json();
                    if (last_ != null) {
                        last.put("id", last_.getId()).put("date", last_.getDate("date")).put("user", last_.getString("user"));
                    } else {
                        last.put("date", Db.findById("Posts", parent.getId()).getDate("date"));
                    }
                    update_.put("$set", new Json("last", last));
                    update_.put("$inc", new Json("replies", data.getBoolean("restore", false) ? 1 : -1));
                    Db.updateOne("Posts", Filters.eq("_id", parent.getId()), update_);
                }
                return new Json("ok", true);
            }
            return new Json("ok", post != null);

        }


        return new Json("ok", false);

    }


    public static Json history(String post_id, int comment) {

        Pipeline pipeline = new Pipeline();
        pipeline.add(Aggregates.match(Filters.and(Filters.eq("_id", post_id))));

        if (comment >= 0) {
            pipeline.add(Aggregates.project(new Json("_id", false).put("comments", true)));
            pipeline.add(Aggregates.project(new Json("changes", new Json("$arrayElemAt", Arrays.asList("$comments", comment)))));
            pipeline.add(Aggregates.project(new Json("changes", "$changes.changes")));
        } else {
            pipeline.add(Aggregates.project(new Json("_id", false).put("changes", true)));
        }

        pipeline.add(Aggregates.unwind("$changes", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("id")));

        pipeline.add(Aggregates.unwind("$changes.docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")));
        pipeline.add(Aggregates.lookup("BlobFiles", "changes.docs", "_id", "changes.docs"));
        pipeline.add(Aggregates.sort(Sorts.ascending("pos_doc")));

        pipeline.add(Aggregates.group("$id", Arrays.asList(
                Accumulators.first("id", "$id"),
                Accumulators.first("user", "$user"),
                Accumulators.first("changes", "$changes"),
                Accumulators.push("changes_docs", new Json("$arrayElemAt", Arrays.asList("$changes.docs", 0)))
        )));

        pipeline.add(Aggregates.lookup("Users", "changes.user", "_id", "changes.user"));
        pipeline.add(Aggregates.unwind("$changes.user", new UnwindOptions().preserveNullAndEmptyArrays(true)));

        pipeline.add(Aggregates.sort(Sorts.ascending("id")));
        pipeline.add(Aggregates.project(
                new Json("changes",
                        new Json("user", new Json("id", "$changes.user._id").put("name", "$changes.user.name").put("avatar", "$changes.user.avatar"))
                                .put("restore", true)
                                .put("remove", true)
                                .put("title", true)
                                .put("text", true)
                                .put("date", true)
                                .put("docs", "$changes_docs")
                )
        ));
        pipeline.add(Aggregates.project(
                new Json("changes",
                        new Json("user", true)
                                .put("restore", true)
                                .put("remove", true)
                                .put("title", true)
                                .put("text", true)
                                .put("date", true)
                                .put("docs", new Json("_id", true).put("type", true).put("size", true).put("text", true))
                )
        ));

        pipeline.add(Aggregates.group(null, List.of(
                Accumulators.push("changes", "$changes")
        )));

        try {
            return Db.aggregate("Posts", pipeline).first();
        } catch (Exception e) {
            e.printStackTrace();
            return new Json("error", "UNKNOWN");
        }
    }

}
