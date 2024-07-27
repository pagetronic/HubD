/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.threads;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UnwindOptions;
import live.page.hubd.blobs.BlobsDb;
import live.page.hubd.content.likes.LikesUtils;
import live.page.hubd.content.users.UsersAggregator;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ThreadsAggregator {

    private static final int number_posts = 20;
    private static final int number_threads = 20;

    private static final Aggregator grouper = new Aggregator(
            "date", "update", "last", "title", "url", "text", "likes", "liked",
            "breadcrumb", "posts", "user", "replies", "lng", "domain", "parent"
    );


    public static Json getThread(String _id, Users user, String paging_str) {
        Pipeline pipeline = new Pipeline();


        pipeline.add(Aggregates.match(Filters.and(Filters.eq("_id", _id), Filters.exists("remove", false))));
        pipeline.add(Aggregates.limit(1));

        pipeline.makeBreadcrumb();

        pipeline.addAll(LikesUtils.addPipelineLiked(user));

        pipeline.addAll(getThreadPostParent(user));

        pipeline.addAll(BlobsDb.getBlobsPipeline(grouper));


        pipeline.add(Aggregates.addFields(new Field<>("url", new Json("$concat", Arrays.asList("/threads/", "$_id")))));

        pipeline.addAll(UsersAggregator.getUserPipeline(grouper, "user", false));


        Paginer paginer = new Paginer(paging_str, "date", number_posts);

        pipeline.add(new Json("$lookup", new Json("from", "Posts").put("as", "posts")
                .put("pipeline", getThreadPostsPipeline(user, Filters.eq("parents.id", _id), paginer))
        ));

        pipeline.add(Aggregates.project(grouper.getProjection()
                .put("domain", Pipeline.getDomainExpose("$lng"))
        ));


        pipeline.add(Aggregates.project(grouper.getProjection().put("title", Pipeline.makeTitle("$", 70))));
        pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

        Json thread = Db.aggregate("Posts", pipeline).first();
        if (thread == null) {
            return null;
        }
        thread.put("posts", paginer.getResult(thread.getListJson("posts")));


        return thread;
    }

    public static Json getThreads(Bson filter, String paging_str, Users user) {

        Paginer paginer = new Paginer(paging_str, "-last.date", number_threads);
        Pipeline pipeline = new Pipeline();

        List<Bson> filters = new ArrayList<>();
        if (filter != null) {
            filters.add(filter);
        }

        filters.add(Filters.exists("remove", false));


        Bson paging = paginer.getFilters();
        if (paging != null) {
            filters.add(paging);
        }

        pipeline.add(Aggregates.match(Filters.and(filters)));
        pipeline.add(paginer.getFirstSort());

        pipeline.add(paginer.getLimit());

        pipeline.addAll(LikesUtils.addPipelineLiked(user));

        pipeline.makeBreadcrumb();
        pipeline.add(Aggregates.addFields(new Field<>("url", new Json("$concat", Arrays.asList("/threads/", "$_id")))));

        pipeline.addAll(UsersAggregator.getUserPipeline(grouper, "user", false));


        pipeline.add(Aggregates.project(grouper.getProjection().put("title", Pipeline.makeTitle("$", 70))));

        pipeline.add(Aggregates.project(grouper.getProjectionOrder()));
        pipeline.add(paginer.getLastSort());

        return paginer.getResult("Posts", pipeline);
    }


    private static List<Bson> getThreadPostParent(Users user) {

        List<Bson> parentPipeline = new ArrayList<>();
        parentPipeline.add(Aggregates.limit(1));
        parentPipeline.addAll(LikesUtils.addPipelineLiked(user));
        parentPipeline.addAll(UsersAggregator.getUserPipeline(grouper, "user", false));
        parentPipeline.add(Aggregates.addFields(new Field<>("url", new Json("$concat", Arrays.asList("/threads/", "$_id")))));
        parentPipeline.add(Aggregates.project(grouper.getProjectionOrder()));

        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(new Json("$lookup",
                        new Json("from", "Posts")
                                .put("localField", "parents.0.id")
                                .put("foreignField", "_id")
                                .put("as", "parent")
                                .put("pipeline", parentPipeline)
                )
        );

        pipeline.add(Aggregates.unwind("$parent", new UnwindOptions().preserveNullAndEmptyArrays(true)));
        pipeline.add(Aggregates.addFields(new Field<>("parent", new Json("$cond", new Json()
                .put("if", new Json("$eq", Arrays.asList("$parent._id", new BsonUndefined())))
                .put("then", "$$REMOVE").put("else", "$parent")
        ))));
        return pipeline;
    }

    private static List<Bson> getThreadPostsPipeline(Users user, Bson filter, Paginer paginer) {

        Pipeline pipeline = new Pipeline();
        List<Bson> filters = new ArrayList<>();

        if (filter != null) {
            filters.add(filter);
        }

        filters.add(Filters.exists("remove", false));

        if (paginer.getFilters() != null) {
            filters.add(paginer.getFilters());
        }

        pipeline.add(Aggregates.match(Filters.and(filters)));

        pipeline.add(paginer.getFirstSort());

        pipeline.add(paginer.getLimit());

        pipeline.addAll(LikesUtils.addPipelineLiked(user));

        pipeline.add(Aggregates.addFields(new Field<>("url", new Json("$concat", Arrays.asList("/threads/", "$_id")))));

        pipeline.addAll(UsersAggregator.getUserPipeline(grouper, "user", false));


        pipeline.addAll(BlobsDb.getBlobsPipeline(grouper));

        pipeline.add(Aggregates.project(grouper.getProjection().put("title", Pipeline.makeTitle("$", 70))));

        pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

        pipeline.add(paginer.getLastSort());

        return pipeline;
    }


}
