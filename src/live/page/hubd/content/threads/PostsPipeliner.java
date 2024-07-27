package live.page.hubd.content.threads;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UnwindOptions;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.PipelinerStore;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.List;


public class PostsPipeliner extends PipelinerStore.PipelineMaker {

    public PostsPipeliner(String type, String lng, Paginer paginer) {
        super(type, paginer);
        addFilter(Filters.and(Filters.eq("lng", lng), Filters.exists("remove", false)));
    }

    @Override
    protected List<Bson> getSearchPipeline() {

        Pipeline pipeline = new Pipeline();

        Aggregator grouper = new Aggregator("title", "intro", "user", "parents", "thread", "score", "photo", "remove", "date", "thread_id", "post_id", "link", "replies", "lng", "domain", "url", "breadcrumb");

        pipeline.add(Aggregates.addFields(
                        new Field<>("intro",
                                new Json("$replaceAll", new Json()
                                        .put("input", new Json("$replaceAll", new Json()
                                                .put("input", new Json("$cond", Arrays.asList(
                                                        new Json("$or", Arrays.asList(new Json("$eq", Arrays.asList("$text", new BsonUndefined())), new Json("$eq", Arrays.asList("$text", null)))),
                                                        null, "$text")))
                                                .put("find", "\n")
                                                .put("replacement", " ")
                                        ))
                                        .put("find", "=")
                                        .put("replacement", ""))
                        ),
                        new Field<>("thread", new Json("$map", new Json("input", "$parents").put("as", "ele").put("in", "$$ele.id")))
                )
        );


        pipeline.add(Aggregates.lookup("Posts", "thread", "_id", "thread"));
        pipeline.add(Aggregates.unwind("$thread", new UnwindOptions().preserveNullAndEmptyArrays(true)));


        pipeline.add(Aggregates.project(grouper.getProjection()
                        .put("_id", new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$thread._id", new BsonUndefined())), "$thread._id", "$_id")))
                        .put("parents", new Json("$cond", Arrays.asList(new Json("$ne", Arrays.asList("$thread.parents", new BsonUndefined())), "$thread.parents", "$parents")))
                        .put("post_id", "$_id")
                )
        );


        pipeline.makeBreadcrumb();
        pipeline.add(Aggregates.addFields(new Field<>("url", new Json("$concat", Arrays.asList("/threads/", "$_id")))));


        pipeline.add(Aggregates.project(grouper.getProjection()
                .put("title", new Json("$cond", Arrays.asList(
                        new Json("$or", Arrays.asList(new Json("$eq", Arrays.asList("$title", null)), new Json("$eq", Arrays.asList("$title", new BsonUndefined())))),
                        "$thread.title", "$title")))
                .put("url", new Json("$concat", Arrays.asList("$url", "#", "$post_id")))
                .put("domain", Pipeline.getDomainExpose("$lng"))
                .put("logo", new Json("$concat", Arrays.asList(Settings.getCDNHttp(),
                                new Json("$cond",
                                        Arrays.asList(new Json("$eq", Arrays.asList("$user.avatar", new BsonUndefined())),
                                                null,
                                                new Json("$concat", Arrays.asList("/files/", "$user.avatar"))))
                        ))
                )
        ));

        return pipeline;
    }

}
