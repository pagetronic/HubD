package live.page.hubd.system.db.utils;

import com.mongodb.client.model.*;
import live.page.hubd.system.Settings;
import live.page.hubd.system.json.Json;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Pipeline extends ArrayList<Bson> {

    private static final List<Json> domains = Settings.getDomainsInfos();

    public static Json getDomainExpose(String lng) {
        return new Json("$arrayElemAt", Arrays.asList(
                new Json("$map", new Json("input", new Json("$filter", new Json("input", domains).put("as", "domains").put("cond", new Json("$eq", Arrays.asList(lng, "$$domains.lng")))))
                        .put("as", "ele").put("in", "$$ele.domain")
                )
                , 0));
    }

    public static Json makeTitle(String key, int length) {

        return new Json("$cond", Arrays.asList(new Json("$or", List.of(
                        new Json("$eq", Arrays.asList(key + "title", null)),
                        new Json("$eq", Arrays.asList(key + "title", new BsonUndefined()))
                )),
                new Json("$cond", new Json()
                        .put("if", new Json("$gt", Arrays.asList(new Json("$strLenCP", key + "text"), length)))
                        .put("then", new Json("$concat", Arrays.asList(

                                new Json("$reduce",
                                        new Json()
                                                .put("input",
                                                        new Json("$reverseArray", new Json("$slice",
                                                                List.of(
                                                                        new Json("$reverseArray", new Json("$split", Arrays.asList(
                                                                                new Json("$substrCP", Arrays.asList(
                                                                                        new Json("$replaceAll", new Json().put("input", key + "text").put("find", "\n").put("replacement", " "))
                                                                                        , 0, length)), " "))),
                                                                        1, 50
                                                                )
                                                        )))
                                                .put("initialValue", "")
                                                .put("in", new Json("$concat", Arrays.asList("$$value", " ", "$$this"))))


                                , "…")))
                        .put("else", key + "text")
                ),
                key + "title"

        ));

    }

    public void makeBreadcrumb() {

        add(Aggregates.addFields(new Field<>("bread_parent", AggregateUtils.arrayElemAt("$parents", 0))));

        add(Aggregates.addFields(new Field<>("page_parent",
                        new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$bread_parent.type", "Pages")),
                                "$bread_parent.id",
                                "$$REMOVE"
                        ))
                ))
        );

        add(Aggregates.addFields(new Field<>("post_parent",
                        new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$bread_parent.type", "Posts")),
                                "$bread_parent.id",
                                "$$REMOVE"
                        ))
                ))
        );

        add(Aggregates.graphLookup("Pages", "$page_parent",
                "parents.0.id", "_id", "breadcrumb_pages", new GraphLookupOptions().depthField("pages_depth").maxDepth(50)));

        add(Aggregates.graphLookup("Posts", "$post_parent",
                "parents.0.id", "_id", "breadcrumb_posts", new GraphLookupOptions().depthField("posts_depth").maxDepth(50)));


        add(Aggregates.addFields(new Field<>("more_parents", new Json("$last", new Json("$sortArray",
                new Json().put("input", "$breadcrumb_posts").put("sortBy", new Json("posts_depth", 1))
        )))));
        add(Aggregates.addFields(new Field<>("more_parents", new Json("$last", "$more_parents.parents"))));

        add(Aggregates.graphLookup("Pages",
                "$more_parents.id",
                "parents.0.id", "_id", "breadcrumb_pages_more", new GraphLookupOptions().depthField("pages_depth_more").maxDepth(50)));


        add(Aggregates.addFields(new Field<>("breadcrumb_pages_ids",
                new Json("$map", new Json("input",
                        new Json("$sortArray",
                                new Json()
                                        .put("input",
                                                new Json("$concatArrays", List.of("$breadcrumb_pages", "$breadcrumb_pages_more"))
                                        )
                                        .put("sortBy", new Json("pages_depth", 1).put("pages_depth_more", 1))
                        )
                ).put("as", "pages").put("in", "$$pages._id"))
        )));

        add(new Json("$lookup",
                        new Json("from", "Pages")
                                .put("localField", "breadcrumb_pages_ids")
                                .put("foreignField", "_id")
                                .put("pipeline",
                                        Arrays.asList(
                                                Aggregates.graphLookup("Pages",
                                                        "$_id",
                                                        "parents.0.id", "_id", "breadcrumb",
                                                        new GraphLookupOptions().depthField("depth").maxDepth(50)),
                                                Aggregates.unwind("$breadcrumb", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                                                Aggregates.sort(new Json("breadcrumb.depth", -1)),
                                                Aggregates.group("$_id", List.of(
                                                        Accumulators.last("id", "$breadcrumb._id"),
                                                        Accumulators.last("title", "$breadcrumb.title"),
                                                        Accumulators.last("text", "$breadcrumb.text"),
                                                        Accumulators.last("lng", "$breadcrumb.lng"),
                                                        Accumulators.push("urls",
                                                                new Json("$cond", new Json()
                                                                        .put("if", new Json("$eq", Arrays.asList(new Json("$size", AggregateUtils.forceArray("$breadcrumb.url")), 0)))
                                                                        .put("then", "$_id")
                                                                        .put("else", new Json("$arrayElemAt", Arrays.asList("$breadcrumb.url", 0)))
                                                                )


                                                        )
                                                )),
                                                Aggregates.addFields(
                                                        new Field<>("url", new Json("$reduce", new Json("input", "$urls")
                                                                .put("initialValue", "")
                                                                .put("in", new Json("$concat", Arrays.asList("$$value", "/", "$$this")))))
                                                )


                                        )
                                )
                                .put("as", "breadcrumb_pages")
                )

        );

        add(Aggregates.addFields(new Field<>("breadcrumb_pages",
                                new Json("$map", new Json("input", "$breadcrumb_pages").put("as", "pages")
                                        .put("in", new Json()
                                                .put("_id", "$$pages._id")
                                                .put("title", "$$pages.title")
                                                .put("url", "$$pages.url")
                                                .put("pages_depth", new Json("$indexOfArray", Arrays.asList("$breadcrumb_pages_ids", "$$pages.id")))
                                                .put("lng", "$$pages.lng")
                                        )
                                )
                        )
                )
        );

        add(Aggregates.addFields(
                new Field<>("breadcrumb",
                        new Json("$sortArray",
                                new Json()
                                        .put("input", new Json("$concatArrays", List.of("$breadcrumb_pages", "$breadcrumb_posts")))
                                        .put("sortBy", new Json("pages_depth", -1).put("pages_depth_more", -1).put("posts_depth", -1))
                        )),
                new Field<>("breadcrumb_pages_ids", "$$REMOVE"),
                new Field<>("breadcrumb_pages", "$$REMOVE")
        ));

        add(Aggregates.addFields(new Field<>("breadcrumb",
                new Json("$map", new Json("input", "$breadcrumb").put("as", "crumb")
                        .put("in",
                                new Json()
                                        .put("id", "$$crumb._id")
                                        .put("type", new Json("$cond", new Json()
                                                .put("if", new Json("$ne", Arrays.asList("$$crumb.posts_depth", new BsonUndefined())))
                                                .put("then", "Posts")
                                                .put("else", "Pages")
                                        ))
                                        .put("title", makeTitle("$$crumb.", 30))
                                        .put("url",
                                                new Json("$cond", new Json()
                                                        .put("if", new Json("$ne", Arrays.asList("$$crumb.url", new BsonUndefined())))
                                                        .put("then", "$$crumb.url")
                                                        .put("else",
                                                                new Json("$concat", Arrays.asList(
                                                                        new Json("$cond", new Json()
                                                                                .put("if", new Json("$ne", Arrays.asList("$$crumb.posts_depth", new BsonUndefined())))
                                                                                .put("then", "/threads/")
                                                                                .put("else", "/pages/")
                                                                        ), "$$crumb._id"))))
                                        )

                                        .put("lng", "$$crumb.lng")
                                        .put("domain", Pipeline.getDomainExpose("$$crumb.lng"))


                        )))));


    }


}
