/*
 * Copyright (c) 2019. PAGE and Sons
 */

package live.page.hubd.blobs;

import com.mongodb.client.model.*;
import live.page.hubd.content.users.UsersAggregator;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.utils.Fx;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlobsDb {


    /**
     * Get user files in DB
     */
    public static Json getChildFiles(String parent, String paging_str) {
        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("parents", parent));
        Json rez = getFiles(filters, paging_str);
        rez.getJson("paging").put("base", "/blobs/" + parent);
        return rez;
    }

    /**
     * Get user files in DB
     */
    public static Json getUserFiles(String user_id, String paging_str) {
        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("user", user_id));
        return getFiles(filters, paging_str);

    }

    /**
     * Get user files in DB
     */
    private static Json getFiles(List<Bson> filters, String paging_str) {
        Aggregator grouper = new Aggregator("name", "type", "size", "date", "src");
        Paginer paginer = new Paginer(paging_str, "-date", 30);
        Bson paging_filter = paginer.getFilters();
        Pipeline pipeline = new Pipeline();
        if (paging_filter != null) {
            filters.add(paging_filter);
        }
        pipeline.add(Aggregates.match(Filters.and(filters)));
        pipeline.add(paginer.getFirstSort());
        pipeline.add(paginer.getLimit());
        pipeline.addAll(UsersAggregator.getUserPipeline(grouper, "user", false));
        pipeline.add(Aggregates.project(grouper.getProjection()
                .put("src", new Json("$concat", Arrays.asList(Settings.getCDNHttp(), "/files/", "$_id")))));
        pipeline.add(Aggregates.project(grouper.getProjectionOrder()));
        pipeline.add(paginer.getLastSort());
        return paginer.getResult("BlobFiles", pipeline);

    }

    /**
     * Simple update file infos
     */
    public static Json updateText(Json data, Users user) {
        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("_id", data.getId()));
        if (!user.getAdmin()) {
            filters.add(Filters.eq("user", user.getId()));
        }
        return new Json("ok", Db.updateOne("BlobFiles", Filters.and(filters), new Json("$set", new Json("text", Fx.normalizePost(data.getString("text"))))).getModifiedCount() > 0);
    }

    /**
     * Remove file
     */
    public static boolean remove(Users user, String id) {
        Json file = Db.findByIdUser("BlobFiles", id, user.getId());
        if (file == null) {
            return false;
        }
        Db.deleteOne("BlobFiles", Filters.eq("_id", file.getId()));
        Db.deleteMany("BlobChunks", Filters.eq("f", file.getId()));
        return true;
    }

    /**
     * Get aggregate pipeline for blob
     */
    public static List<Bson> getBlobsPipeline(Aggregator grouper) {
        Pipeline pipeline = new Pipeline();
        pipeline.add(Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")));
        pipeline.add(Aggregates.lookup("BlobFiles", "docs", "_id", "docs"));
        pipeline.add(Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true)));
        pipeline.add(Aggregates.project(grouper.getProjection()
                .put("docs", new Json("_id", true)
                        .put("src", new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$docs._id")))
                        .put("size", true).put("text", true))
        ));
        pipeline.add(Aggregates.sort(Sorts.ascending("pos_doc")));

        pipeline.add(Aggregates.group("$_id", grouper.getGrouper(Accumulators.push("docs", "$docs"))));
        pipeline.add(Aggregates.project(grouper.getProjection()
                .put("docs", new Json("$filter", new Json("input", "$docs").put("as", "docs").put("cond", new Json("$ne", Arrays.asList("$$docs._id", new BsonUndefined())))))
        ));
        return pipeline;
    }
}
