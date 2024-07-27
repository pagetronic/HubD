/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.pages;

import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.*;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.utils.Fx;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PagesUtils {

    public static Json revision(String _id) {

        Json projection = new Json().put("title", true).put("top_title", true)
                .put("intro", true).put("text", true).put("user", true).put("editor", true)
                .put("url", true).put("remove", true).put("origine", true)
                .put("edit", true).put("date", true).put("update", true).put("keywords", true)
                .put("docs", true).put("parents", true).put("parents_", true).put("children", true).put("users", true);

        Json revision = Db.aggregate("Revisions",
                Arrays.asList(
                        Aggregates.match(Filters.eq("_id", _id)),

                        Aggregates.sort(Sorts.ascending("edit")),
                        Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")),
                        Aggregates.lookup("BlobFiles", "docs", "_id", "docs"),
                        Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                        Aggregates.sort(Sorts.ascending("pos_doc")),
                        Aggregates.group("$_id", Arrays.asList(
                                Accumulators.first("users", "$users"),
                                Accumulators.first("date", "$date"),
                                Accumulators.first("title", "$title"),
                                Accumulators.first("top_title", "$top_title"),
                                Accumulators.first("intro", "$intro"),
                                Accumulators.first("text", "$text"),
                                Accumulators.first("editor", "$editor"),
                                Accumulators.first("edit", "$edit"),
                                Accumulators.first("remove", "$remove"),
                                Accumulators.first("origine", "$origine"),
                                Accumulators.first("url", "$url"),
                                Accumulators.first("children", "$children"),
                                Accumulators.first("parents", "$parents"),
                                Accumulators.first("keywords", "$keywords"),
                                Accumulators.push("docs", "$docs")
                        )),

                        Aggregates.lookup("Pages", "children", "_id", "children"),
                        Aggregates.unwind("$parents", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos")),
                        Aggregates.lookup("Pages", "parents", "_id", "parents"),
                        Aggregates.lookup("Users", "editor", "_id", "editor"),

                        Aggregates.unwind("$editor", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                        Aggregates.unwind("$parents", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                        Aggregates.sort(Sorts.ascending("pos")),
                        Aggregates.group("$_id", Arrays.asList(
                                Accumulators.first("users", "$users"),
                                Accumulators.first("date", "$date"),
                                Accumulators.first("title", "$title"),
                                Accumulators.first("top_title", "$top_title"),
                                Accumulators.first("intro", "$intro"),
                                Accumulators.first("text", "$text"),
                                Accumulators.first("editor", "$editor"),
                                Accumulators.first("edit", "$edit"),
                                Accumulators.first("remove", "$remove"),
                                Accumulators.first("origine", "$origine"),
                                Accumulators.first("docs", "$docs"),
                                Accumulators.first("url", "$url"),
                                Accumulators.first("children", "$children"),
                                Accumulators.first("parents_", "$parents"),
                                Accumulators.first("keywords", "$keywords"),
                                Accumulators.push("parents", "$parents")
                        )),

                        Aggregates.unwind("$users", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("users_pos")),
                        Aggregates.lookup("Users", "users", "_id", "users"),
                        Aggregates.sort(Sorts.ascending("users_pos")),
                        Aggregates.group("$_id", Arrays.asList(
                                Accumulators.push("users", new Json("$arrayElemAt", Arrays.asList("$users", 0))),
                                Accumulators.first("date", "$date"),
                                Accumulators.first("title", "$title"),
                                Accumulators.first("top_title", "$top_title"),
                                Accumulators.first("intro", "$intro"),
                                Accumulators.first("text", "$text"),
                                Accumulators.first("editor", "$editor"),
                                Accumulators.first("edit", "$edit"),
                                Accumulators.first("remove", "$remove"),
                                Accumulators.first("origine", "$origine"),
                                Accumulators.first("docs", "$docs"),
                                Accumulators.first("url", "$url"),
                                Accumulators.first("children", "$children"),
                                Accumulators.first("parents", "$parents"),
                                Accumulators.first("keywords", "$keywords"),
                                Accumulators.first("parents_", "$parents_")
                        )),

                        Aggregates.sort(Sorts.ascending("edit")),
                        Aggregates.project(projection.clone()
                                .put("users", new Json("_id", true).put("name", true).put("avatar", true))
                                .put("parents", new Json("$filter", new Json("input", "$parents").put("as", "parents_").put("cond", new Json("$ne", Arrays.asList("$$parents_._id", null)))))
                                .put("children", new Json("$filter", new Json("input", "$children").put("as", "children").put("cond", new Json("$ne", Arrays.asList("$children._id", null)))))
                                .put("editor", new Json("name", true).put("_id", true).put("avatar", true))
                                .put("docs", new Json("_id", true).put("type", true).put("size", true).put("text", true))
                                .put("keywords", new Json("$reduce", new Json("input", "$keywords").put("initialValue", "")
                                        .put("in", new Json("$concat", Arrays.asList("$$value", ", ", "$$this")))))
                        ),
                        Aggregates.project(projection.clone()
                                .remove("parents_")
                                .put("children",
                                        new Json("title", true).put("url", true).put("_id", true)
                                )
                                .put("parents",
                                        new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$parents_", null)), null, "$parents"))
                                )
                                .put("keywords",
                                        new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$substr", Arrays.asList("$keywords", 0, 2)), ", ")), new Json("$substr", Arrays.asList("$keywords", 2, -1)), "$keywords"))
                                )
                        )

                )
        ).first();
        return revision;
    }

    public static Json revision(String url, String domain, String _id, Users user) {

        if ((_id == null && url == null) || (_id != null && url != null)) {
            return null;
        }
        String[] urls = null;
        if (url != null) {
            urls = url.split("/");
        }
        Json page = PagesAggregator.getPage(_id != null ?
                Filters.eq("_id", Db.findById("Revisions", _id).getString("origine")) :
                Filters.and(Filters.eq("url", urls[urls.length - 1]), Filters.eq("lng", Settings.getLang(domain))), null, user);

        if (page == null) {

            List<Bson> filters = new ArrayList<>();
            if (url != null) {
                filters.add(Filters.and(Filters.exists("remove", true)));
                filters.add(Filters.eq("url", urls[urls.length - 1]));
            } else {
                filters.add(Filters.eq("_id", _id));
            }

            Json removed = Db.find("Revisions", Filters.and(filters)).first();
            if (removed != null) {
                page = new Json("_id", removed.getString("origine"));
            } else {
                page = new Json();
            }
        }

        Json revision = page.clone();
        List<Json> prev_children = revision.getListJson("children");
        List<Json> prev_docs = revision.getListJson("docs");
        revision.remove("children").remove("docs");

        revision.put("page", page);

        Json projection = new Json().put("title", true).put("top_title", true)
                .put("intro", true).put("text", true).put("user", true).put("editor", true)
                .put("url", true).put("remove", true)
                .put("edit", true).put("date", true).put("update", true).put("keywords", true)
                .put("docs", true).put("parents", true).put("parents_", true).put("children", true).put("users", true);

        List<Json> revisions = Db.aggregate("Revisions",
                Arrays.asList(
                        Aggregates.match(Filters.eq("origine", page.getId())),

                        Aggregates.sort(Sorts.ascending("edit")),
                        Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")),
                        Aggregates.lookup("BlobFiles", "docs", "_id", "docs"),
                        Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                        Aggregates.sort(Sorts.ascending("pos_doc")),
                        Aggregates.group("$_id", Arrays.asList(
                                Accumulators.first("users", "$users"),
                                Accumulators.first("date", "$date"),
                                Accumulators.first("title", "$title"),
                                Accumulators.first("top_title", "$top_title"),
                                Accumulators.first("intro", "$intro"),
                                Accumulators.first("text", "$text"),
                                Accumulators.first("editor", "$editor"),
                                Accumulators.first("edit", "$edit"),
                                Accumulators.first("remove", "$remove"),
                                Accumulators.first("url", "$url"),
                                Accumulators.first("keywords", "$keywords"),

                                Accumulators.first("children", "$children"),
                                Accumulators.first("parents", "$parents"),
                                Accumulators.push("docs", "$docs")
                        )),

                        Aggregates.unwind("$parents", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos")),
                        Aggregates.lookup("Pages", "parents", "_id", "parents"),
                        Aggregates.lookup("Users", "editor", "_id", "editor"),

                        Aggregates.unwind("$editor", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                        Aggregates.unwind("$parents", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                        Aggregates.sort(Sorts.ascending("pos")),
                        Aggregates.group("$_id", Arrays.asList(
                                Accumulators.first("users", "$users"),
                                Accumulators.first("date", "$date"),
                                Accumulators.first("title", "$title"),
                                Accumulators.first("top_title", "$top_title"),
                                Accumulators.first("intro", "$intro"),
                                Accumulators.first("text", "$text"),
                                Accumulators.first("editor", "$editor"),
                                Accumulators.first("edit", "$edit"),
                                Accumulators.first("remove", "$remove"),
                                Accumulators.first("docs", "$docs"),
                                Accumulators.first("url", "$url"),
                                Accumulators.first("keywords", "$keywords"),
                                Accumulators.first("children", "$children"),
                                Accumulators.first("parents_", "$parents"),
                                Accumulators.push("parents", "$parents")
                        )),

                        Aggregates.unwind("$users", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("users_pos")),
                        Aggregates.lookup("Users", "users", "_id", "users"),
                        Aggregates.sort(Sorts.ascending("users_pos")),
                        Aggregates.group("$_id", Arrays.asList(
                                Accumulators.push("users", new Json("$arrayElemAt", Arrays.asList("$users", 0))),
                                Accumulators.first("date", "$date"),
                                Accumulators.first("title", "$title"),
                                Accumulators.first("top_title", "$top_title"),
                                Accumulators.first("intro", "$intro"),
                                Accumulators.first("text", "$text"),
                                Accumulators.first("editor", "$editor"),
                                Accumulators.first("edit", "$edit"),
                                Accumulators.first("remove", "$remove"),
                                Accumulators.first("docs", "$docs"),
                                Accumulators.first("url", "$url"),
                                Accumulators.first("keywords", "$keywords"),
                                Accumulators.first("children", "$children"),
                                Accumulators.first("parents", "$parents"),
                                Accumulators.first("parents_", "$parents_")
                        )),

                        Aggregates.sort(Sorts.ascending("edit")),
                        Aggregates.project(projection.clone()
                                .put("users", new Json("_id", true).put("name", true).put("avatar", true))
                                .put("parents", new Json("$filter", new Json("input", "$parents").put("as", "parents_").put("cond", new Json("$ne", Arrays.asList("$$parents_._id", null)))))
                                .put("editor", new Json("name", true).put("_id", true).put("avatar", true))
                                .put("docs", new Json("_id", true).put("type", true).put("size", true).put("text", true))
                                .put("keywords", new Json("$reduce", new Json("input", "$keywords").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", ", ", "$$this")))))

                        ),
                        Aggregates.project(projection.clone()
                                .remove("parents_")
                                .put("keywords",
                                        new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$substr", Arrays.asList("$keywords", 0, 2)), ", ")), new Json("$substr", Arrays.asList("$keywords", 2, -1)), "$keywords"))
                                )
                                .put("parents",
                                        new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$parents_", null)), null, "$parents"))
                                )
                        )
                )
        ).into(new ArrayList<>());

        List<String> keys = Arrays.asList("date", "top_title", "title", "intro", "text", "url", "parents", "keywords", "children", "docs", "users");
        revisions.add(page);
        for (Json modification : revisions) {
            for (String key : keys) {
                if (modification.get(key) != null &&
                        (!List.class.isAssignableFrom(modification.get(key).getClass()) || ((List) modification.get(key)).size() > 0)) {

                    revision.put(key, modification.get(key));
                }
            }

            revision.put("remove", modification.getBoolean("remove", false));

            if (modification.getId() != null && modification.getId().equals(_id)) {
                modification.put("selected", true);
                break;
            }
        }
        Collections.reverse(revisions);
        if (_id == null && revisions.size() > 0) {
            revisions.get(0).put("selected", true);
        }

        if (revision.containsKey("date")) {
            revision.put("date", new SimpleDateFormat(Fx.ISO_DATE).format(revision.getDate("date")));
        }
        if (revision.containsKey("url")) {
            String[] urls_rev = revision.getString("url").split("/");
            revision.put("url", urls_rev[urls_rev.length - 1]);
        }
        revision.put("revisions", revisions);

        List<Json> children = page.getListJson("children");
        List<String> children_sort = new ArrayList<>();
        List<Object> children_sort_ = revision.getList("children");
        if (children_sort_ != null) {
            children_sort_.forEach(child -> children_sort.add(child.getClass().isAssignableFrom(String.class) ? children.toString() : ((Json) children).getId()));
        }
        if (children != null) {
            List<Json> real_children = new ArrayList<>();
            for (String chidren_id : children_sort) {
                for (Json child : children) {
                    if (chidren_id.equals(child.getId())) {
                        real_children.add(child);
                        children.remove(child);
                    }
                }
            }
            if (!children.isEmpty()) {
                real_children.addAll(children);
            }
            revision.put("children", real_children);
        } else {
            revision.put("children", children);
        }

        if (revision.get("children") == null) {
            revision.put("children", prev_children);
        }

        if (revision.get("docs") == null) {
            revision.put("docs", prev_docs);
        }

        if (url != null && revision.getId() == null) {
            revision.put("url", Fx.cleanURL(urls[urls.length - 1]));
            if (urls.length > 1) {
                Json parent = Db.find("Pages", Filters.eq("url", urls[urls.length - 2])).first();
                if (parent != null) {
                    revision.add("parents", new Json("id", parent.getId()).put("title", parent.getString("title")));
                }
            }
        }
        return revision;
    }

    public static Json draft(Json data, Users user) {
        if (data.getBoolean("remove", false)) {
            return new Json("ok", Db.updateOne("Revisions", Filters.eq("_id", data.getString("revision", "")), new Json("$set", new Json("origine", "draft_removed"))).getModifiedCount() > 0);
        }

        Json revision = new Json("origine", "draft");
        if (!data.getString("revision", "").isEmpty()) {
            revision = Db.findById("Revisions", data.getString("revision", ""));
        }
        revision.put("url", data.getString("url", ""));
        revision.put("lng", Settings.getLang(data.getString("domain")));
        revision.put("top_title", data.getString("top_title", ""));
        revision.put("title", data.getString("title", ""));
        revision.put("intro", data.getText("intro", ""));
        revision.put("text", data.getText("text", ""));
        revision.put("children", data.getList("children"));
        revision.put("parents", data.getList("parents"));
        revision.put("keywords", data.getList("keywords"));
        revision.put("users", data.getList("users"));
        revision.put("editor", user.getId());
        revision.put("edit", new Date());

        revision.put("docs", data.getList("docs"));

        Db.save("Revisions", revision);

        return new Json("ok", true).put("url", "draft");
    }

    public static Json save(Json data, Users user) {
        Date date = new Date();
        boolean restore = false;

        Json page = null;
        if (data.getId() != null) {
            page = Db.findById("Pages", data.getId());

            if (page != null &&
                    !Db.exists("Revisions",
                            Filters.and(Filters.ne("parents", null), Filters.ne("url", null), Filters.eq("origine", page.getId())))
            ) {
                Db.save("Revisions",
                        page.clone().put("origine", page.getId()).remove("_id")
                                .put("edit", page.getDate("date"))
                );
            }

            if (page == null) {

                Json revision = Db.find("Revisions", Filters.eq("origine", data.getId())).sort(Sorts.descending("edit")).first();
                if (revision == null) {
                    return new Json("error", "PAGE_UNKNOWN");
                }
                page = new Json();
                page.put("_id", revision.get("origine"));
                page.put("users", revision.get("users"));
                page.put("date", revision.get("date"));
                page.put("update", revision.get("update"));
                page.put("url", revision.get("url"));
                page.put("top_title", revision.get("top_title"));
                page.put("title", revision.get("title"));
                page.put("intro", revision.get("intro"));
                page.put("text", revision.get("text"));
                page.put("keywords", revision.get("keywords"));
                page.put("parents", revision.get("parents"));
                page.put("children", revision.get("children"));
                page.put("docs", revision.get("docs"));
                restore = true;

            }
        }

        if (data.getBoolean("remove", false)) {
            if (page != null) {
                if (!Db.exists("Pages", Filters.eq("parents", page.getId()))) {
                    Db.save("Revisions", new Json()
                            .put("origine", page.getId()).put("edit", date).put("editor", user.getId())
                            .put("url", page.getString("url"))
                            .put("remove", true));
                    Db.deleteOne("Pages", Filters.eq("_id", page.getId()));

                    return new Json("ok", true);
                } else {
                    return new Json("error", "CHILDRENS_EXISTS");
                }
            }
            return new Json("error", "UNKNOWN");
        }

        for (String key : Arrays.asList("top_title", "title", "intro", "text")) {
            if (data.get(key) != null) {
                data.put(key, Normalizer.normalize(data.getText(key), Normalizer.Form.NFKC));
            }
        }

        List<Json> errors = new ArrayList<>();

        if (!data.getString("url", "").isEmpty()) {
            data.put("url", Fx.cleanURL(data.getString("url", "")));
        }

        if (data.getString("url", "").isEmpty()) {
            String url = Fx.cleanURL(data.getString("title", ""));
            while (Db.exists("Pages", Filters.eq("url", url))) {
                url += "~";
            }
            data.put("url", url);
        } else if (Db.exists("Pages", Filters.and(Filters.eq("lng", Settings.getLang(data.getString("domain"))), Filters.eq("url", data.getString("url")))) &&
                !Db.exists("Pages", Filters.and(Filters.eq("_id", data.getId()), Filters.and(Filters.eq("lng", Settings.getLang(data.getString("domain"))), Filters.eq("url", data.getString("url")))))) {
            errors.add(new Json("element", "url").put("message", "URL_EXIST"));
        }


        if (data.getString("title", "").length() == 0) {
            errors.add(new Json("element", "title").put("message", "NO_TITLE"));
        } else if (data.getString("title").length() < 2) {
            errors.add(new Json("element", "title").put("message", "TITLE_TOO_SHORT"));
        }

        List<String> children = data.getList("children");
        if (children != null && !data.getString("origine", "").isEmpty() && children.contains(data.getString("origine"))) {
            errors.add(new Json("element", "children").put("message", "SAME_ID"));
        }

        List<String> parents = data.getList("parents");
        if (parents != null) {
            if (!data.getString("origine", "").isEmpty() && parents.contains(data.getString("origine"))) {
                errors.add(new Json("element", "parents").put("message", "SAME_ID"));
            }

            for (String parent_id : parents) {
                if (!Db.exists("Pages", Filters.eq("_id", parent_id))) {
                    errors.add(new Json("element", "parents").put("message", "PARENT_NOT_EXIST:" + parent_id));
                }
            }

        } else {
            parents = new ArrayList<>();
        }
        if (!errors.isEmpty()) {
            return new Json("errors", errors);
        }

        List<String> docs = data.getList("docs");

        String text = data.getText("text");

        if (text != null && docs != null) {
            Pattern pattern = Pattern.compile("\\[Photos\\(([a-z0-9]+)\\)\\|?([^\\]]+)\\]");
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String key_id = matcher.group(1);
                if (!docs.contains(key_id)) {
                    docs.add(key_id);
                }
            }
        }
        if (page == null) {
            page = new Json().put("user", user.getId()).put("date", date);
            if (!data.getString("origine", "").isEmpty()) {
                page.put("_id", data.getString("origine"));
            }
        }
        page.put("update", date);


        Json revision = new Json();

        if (restore || (children != null && !children.equals(page.getList("children")))) {
            revision.put("children", children);
            page.put("children", children);
        }
        List<String> users = data.getList("users");
        if (users != null && (restore || !users.equals(page.getList("users")))) {
            revision.put("users", users);
            page.put("users", users);
        }
        if (restore || !page.getString("top_title", "").equals(data.getString("top_title"))) {
            revision.put("top_title", data.getString("top_title"));
            page.put("top_title", data.getString("top_title"));
        }

        if (restore || !page.getString("title", "").equals(data.getString("title"))) {
            revision.put("title", data.getString("title"));
            page.put("title", data.getString("title"));
        }
        if (restore || !page.getString("url", "").equals(data.getString("url"))) {

            revision.put("url", data.getString("url", ""));
            page.put("url", data.getString("url", ""));
        }
        if (restore || !page.getText("intro", "").equals(data.getText("intro"))) {
            revision.put("intro", data.getText("intro"));
            page.put("intro", data.getText("intro"));
        }
        boolean doKeywords = false;
        List<String> page_keywords = page.getList("keywords");
        List<String> data_keywords = Arrays.asList(data.getText("keywords", "").split(" ?, ?"));
        if (restore || !data_keywords.equals(page_keywords)) {
            revision.put("keywords", data_keywords);
            page.put("keywords", data_keywords);
            doKeywords = true;
        }

        if (restore || !page.getString("lng", "").equals(data.getString("lng"))) {
            revision.put("lng", Settings.getLang(data.getString("domain")));
            page.put("lng", Settings.getLang(data.getString("domain")));
        }


        if (restore || !page.getText("text", "").equals(text)) {
            revision.put("text", text);
            page.put("text", text);
        }
        if (restore || page.getList("parents") == null || !page.getList("parents").equals(parents)) {
            revision.put("parents", parents);
            page.put("parents", parents);
        }
        if (restore || page.getList("docs") == null || !page.getList("docs").equals(docs)) {
            revision.put("docs", docs);
            page.put("docs", docs);
        }
        if (revision.isEmpty()) {
            return new Json("error", "NO_MODIFICATION");
        }

        revision.put("edit", date);
        revision.put("editor", user.getId());
        if (page.getId() == null) {
            page.put("_id", Db.getKey());
        }
        Db.updateOne("Pages", Filters.eq("_id", page.getId()), new Json("$set", page), new UpdateOptions().upsert(true));
        revision.put("origine", page.getId());
        Db.save("Revisions", revision);

        if (data.getString("draft", "").equals("draft")) {
            Db.updateOne("Revisions", Filters.and(Filters.eq("_id", data.getString("revision")), Filters.eq("origine", "draft")), new Json("$set", new Json("origine", page.getId())));
        }

        if (children != null) {
            for (String children_id : children) {
                Json child = Db.findOneAndUpdate("Pages", Filters.and(Filters.eq("_id", children_id), Filters.ne("parents", page.getId())), new Json("$push", new Json("parents", page.getId())));
                if (child != null) {
                    Db.save("Revisions", new Json().put("edit", date).put("user", user.getId()).put("parents", child.getList("parents")).put("origine", children_id));
                }
            }
        }

        if (doKeywords) {
            PagesAutoLink.keywords(page.getId(), page.getList("keywords"), user);
        }
        return new Json("ok", true).put("url", page.getString("url"));
    }

    public static Json addForum(String id, String forum_id) {
        return new Json("ok", Db.updateOne("Pages", Filters.and(Filters.eq("_id", id), Filters.ne("forums", forum_id)), Updates.push("forums", forum_id)).getModifiedCount() > 0);
    }

    public static Json remove(String page_id, String forum_id) {
        if (Db.updateOne("Pages", Filters.and(Filters.eq("_id", page_id), Filters.eq("forums", forum_id)), Updates.pull("forums", forum_id)).getModifiedCount() > 0) {
            return new Json("ok", true);
        }
        return new Json("ok", false);
    }

    public static Json sortForums(String id, List<String> forums) {

        if (forums != null && Db.updateOne("Pages", Filters.eq("_id", id), Updates.set("forums", forums)).getModifiedCount() > 0) {
            return new Json("ok", true);
        }
        return new Json("ok", false);
    }

    public static Json search(Json data) {


        int limit = 40;

        Pipeline pipeline = new Pipeline();
        List<Bson> filters = new ArrayList<>();
        Json paging = new Json();

        if (!data.getString("search", "").isEmpty()) {
            filters.add(Filters.or(Filters.regex("title", Pattern.compile(data.getString("search"), Pattern.CASE_INSENSITIVE)), Filters.text(data.getString("search"))));
        }
        if (data.containsKey("filter") && data.get("filter") != null) {
            filters.add(Filters.nin("_id", data.getList("filter")));
        }

        if (filters.size() > 0) {
            pipeline.add(Aggregates.match(Filters.and(filters)));
        }

        if (!data.getString("search", "").isEmpty()) {
            pipeline.add(Aggregates.project(new Json("_id", false).put("score", new Json("$meta", "textScore")).put("id", "$_id").put("title", new Json("$concat", Arrays.asList("$title", " (", "$lng", ")"))).put("update", true)));
            pipeline.add(Aggregates.sort(Sorts.orderBy(Sorts.metaTextScore("score"), Sorts.ascending("_id"))));
        } else {
            pipeline.add(Aggregates.project(new Json("_id", false).put("id", "$_id").put("title", new Json("$concat", Arrays.asList("$title", " (", "$lng", ")"))).put("update", true)));
            pipeline.add(Aggregates.sort(Sorts.orderBy(Sorts.descending("update"), Sorts.ascending("_id"))));
        }
        pipeline.add(Aggregates.limit(limit + 1));

        List<Json> pages = Db.aggregate("Pages", pipeline).into(new ArrayList<>());

        if (pages.size() > limit) {
            pages = pages.subList(0, pages.size() - 2);
        }
        return new Json("result", pages).put("paging", paging);
    }

    public static Json addParents(String id, String parent_id, Users user) {
        Json page = Db.findOneAndUpdate("Pages", Filters.and(Filters.eq("_id", id), Filters.ne("parents", parent_id)), Updates.push("parents", parent_id), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        if (page != null) {
            Db.save("Revisions", new Json("edit", new Date()).put("parents", page.get("parents")).put("editor", user.getId()).put("origine", id));

            return new Json("ok", true);
        }
        return new Json("ok", false);
    }

    public static Json removeParents(String id, String parent_id, Users user) {
        Json page = Db.findOneAndUpdate("Pages", Filters.and(Filters.eq("_id", id), Filters.eq("parents", parent_id)), Updates.pull("parents", parent_id), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

        if (page != null) {
            Db.save("Revisions", new Json("edit", new Date()).put("parents", page.get("parents")).put("editor", user.getId()).put("origine", id));
            return new Json("ok", true);
        }
        return new Json("error", "DONT_HAVE_PARENT");

    }

    public static Json sortParents(String id, List<String> parents, Users user) {
        Json page = Db.findOneAndUpdate("Pages", Filters.eq("_id", id), Updates.set("parents", parents), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

        if (page != null) {
            Db.save("Revisions", new Json("edit", new Date()).put("parents", page.get("parents")).put("editor", user.getId()).put("origine", id));

            return new Json("ok", true);
        }
        return new Json("ok", false);
    }

    public static Json addChildrens(String id, String children_id, Users user) {
        Json page = Db.findOneAndUpdate("Pages", Filters.and(Filters.eq("_id", children_id), Filters.ne("parents", id)), Updates.push("parents", id), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        if (page != null) {
            Db.save("Revisions", new Json("edit", new Date()).put("parents", page.get("parents")).put("editor", user.getId()).put("origine", children_id));
            return new Json("ok", true);
        }
        return new Json("ok", false);
    }

    public static Json removeChildrens(String id, String children_id, Users user) {
        Json page = Db.findOneAndUpdate("Pages", Filters.and(Filters.eq("_id", children_id), Filters.eq("parents", id)), Updates.pull("parents", id), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

        if (page != null) {
            if (!hasParent(children_id)) {
                Db.updateOne("Pages", Filters.eq("_id", children_id), Updates.push("parents", id));
                return new Json("error", "ONE_PARENT_NEEDED");
            }
            Db.save("Revisions", new Json("edit", new Date()).put("parents", page.get("parents")).put("editor", user.getId()).put("origine", children_id));

            return new Json("ok", true);
        }
        return new Json("error", "DONT_HAVE_CHILDREN");
    }

    public static Json sortChildrens(String id, List<String> children, Users user) {
        Json page = Db.findOneAndUpdate("Pages", Filters.eq("_id", id), Updates.set("children", children), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        if (page != null) {
            Db.save("Revisions", new Json("edit", new Date()).put("children", page.get("children")).put("editor", user.getId()).put("origine", id));
            return new Json("ok", true);
        }
        return new Json("ok", false);
    }

    public static boolean hasParent(String id) {

        return Db.aggregate("Pages", Arrays.asList(
                Aggregates.match(Filters.eq("_id", id)),
                Aggregates.lookup("Pages", "parents", "_id", "parents"),
                Aggregates.unwind("$parents", new UnwindOptions()))).into(new ArrayList<>()).size() > 0;
    }

    public static Json childrenPage(String parent) {

        Json rez = new Json();
        Pipeline pipeline = new Pipeline();
        if (parent == null || (parent != null && parent.isEmpty())) {
            parent = "ROOT";
        }
        if (parent.equals("ROOT")) {
            pipeline.add(Aggregates.match(Filters.or(Filters.eq("parents", null), Filters.eq("parents", new ArrayList<>()), Filters.eq("parents", "ROOT"))));
            pipeline.add(Aggregates.sort(Sorts.ascending("date")));
            pipeline.add(Aggregates.project(new Json().put("_id", false).put("id", "$_id").put("title", true)));
        } else {
            pipeline.add(Aggregates.match(Filters.eq("_id", parent)));
            pipeline.add(Aggregates.project(new Json().put("children_", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$children", new BsonUndefined())),
                    new ArrayList<>(),
                    "$children")
            ))));

            pipeline.add(Aggregates.lookup("Pages", "_id", "parents", "children"));
            pipeline.add(Aggregates.unwind("$children"));

            pipeline.add(Aggregates.project(new Json().put("_id", false).put("id", "$_id").put("children_", true)
                    .put("children", new Json("id", "$children._id").put("title", true).put("order", new Json("$indexOfArray", Arrays.asList("$children_", "$children._id"))))
            ));
            pipeline.add(Aggregates.replaceRoot("$children"));
        }
        pipeline.add(Aggregates.lookup("Pages", "id", "parents", "children"));
        pipeline.add(Aggregates.sort(Sorts.ascending("order")));
        pipeline.add(Aggregates.project(new Json().put("id", "$id").put("title", "$title").put("children", new Json("$size", "$children"))));

        rez.put("result", Db.aggregate("Pages", pipeline).into(new ArrayList<>()));
        return rez;
    }

    public static MongoIterable<Json> getDrafts() {

        Pipeline pipeline = new Pipeline();
        pipeline.add(Aggregates.match(Filters.eq("origine", "draft")));

        pipeline.add(Aggregates.lookup("Users", "editor", "_id", "editor"));
        pipeline.add(Aggregates.unwind("$editor", new UnwindOptions().preserveNullAndEmptyArrays(true)));
        pipeline.add(Aggregates.group("$_id", Arrays.asList(Accumulators.first("users", "$users"),
                Accumulators.first("edit", "$edit"),
                Accumulators.first("title", "$title"),
                Accumulators.first("top_title", "$top_title"),
                Accumulators.first("intro", "$intro"),
                Accumulators.first("text", "$text"),
                Accumulators.first("editor", "$editor"),
                Accumulators.first("docs", "$docs"),
                Accumulators.first("url", "$url"),
                Accumulators.first("children", "$children"),
                Accumulators.push("parents", "$parents"))));

        pipeline.add(Aggregates.sort(Sorts.descending("edit")));

        return Db.aggregate("Revisions", pipeline);
    }

    public static int getDraftsCount() {
        return (int) Db.count("Revisions", Filters.eq("origine", "draft"));
    }

    public static String getPossibleRedirect(String uri) {
        String clean = uri.replaceAll(".*/([^/.]+)(/|\\.json|\\.xml|\\.xhtml|\\.html|\\.mob)?$", "$1");
        Json revision = Db.find("Revisions", Filters.and(Filters.eq("url", clean))).sort(Sorts.descending("edit")).first();
        if (revision != null && !revision.getString("301", "").isEmpty()) {
            return revision.getString("301");
        }
        return null;
    }

    /**
     * Get the keywords of a Page
     *
     * @param id of the Page
     * @return the keywords
     */
    public static Json getKeywords(String id) {
        Json page = Db.findById("Pages", id);
        if (page == null || page.getList("keywords") == null) {
            return new Json("keywords", new ArrayList<>());
        }
        return new Json("keywords", page.getList("keywords"));
    }

    /**
     * Set keywords to a Pages and update all Pages
     *
     * @param id       of the Page
     * @param keywords to set to the Page
     * @param user     do the update
     * @return Pages liked
     */
    public static Json keywords(String id, List<String> keywords, Users user) {
        if (id == null || keywords == null || user == null) {
            return new Json("ok", false);
        }
        Db.updateOne("Pages", Filters.eq("_id", id), new Json("$set", new Json("keywords", keywords)));
        Db.save("Revisions", new Json().put("origine", id).put("editor", user.getId()).put("keywords", keywords).put("edit", new Date()));

        return new Json("ok", true).put("links", PagesAutoLink.keywords(id, keywords, user));

    }
}
