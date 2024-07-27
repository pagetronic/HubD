package live.page.hubd.content.pages;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.PipelinerStore;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.List;

public class PagesPipelines extends PipelinerStore.PipelineMaker {


    public PagesPipelines(String type, String lng, Paginer paginer) {
        super(type, paginer);
        addFilter(Filters.eq("lng", lng));
    }

    @Override
    protected List<Bson> getSearchPipeline() {

        Aggregator grouper = new Aggregator("date", "title", "intro", "score", "breadcrumb", "lng", "domain", "url", "parents");

        Pipeline pipeline = new Pipeline();

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
                ))
        );
        pipeline.add(Aggregates.addFields(new Field<>("intro",
                new Json("$cond", new Json()
                        .put("if", new Json("$gt", Arrays.asList(new Json("$strLenCP", "$intro"), 255)))
                        .put("then", new Json("$concat", Arrays.asList(new Json("$substrCP", Arrays.asList("$intro", 0, 255)), "...")))
                        .put("else", "$intro")
                ))));


        pipeline.makeBreadcrumb();
        pipeline.add(PagesAggregator.makeUrl());


        return pipeline;
    }


}