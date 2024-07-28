/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.pages;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UnwindOptions;
import live.page.hubd.content.threads.ThreadsAggregator;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.AggregateUtils;
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

public class PagesAggregator {

    public static Json getPageDomainLng(String url, String lngOrDomain, String paging_str, Users user) {
        String lng;
        if (Settings.getLangs().contains(lngOrDomain)) {
            lng = lngOrDomain;
        } else {
            lng = Settings.getLang(lngOrDomain);
            if (lng == null) {
                lng = Settings.getLangs().get(0);
            }
        }
        return getPage(url, lng, paging_str, user);
    }

    public static Json getPage(String url, String lng, String paging_str, Users user) {
        if (url == null) {
            return null;
        }
        String clean = url.replaceAll(".*/([^/.]+)(/|\\.json|\\.xml|\\.xhtml|\\.html|\\.mob)?$", "$1");
        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("url", clean));
        if (lng != null) {
            filters.add(Filters.eq("lng", lng));
        }
        return PagesAggregator.getPage(Filters.and(filters), paging_str, user);
    }

    public static Json getPage(Bson filter, String paging_str, Users user) {

        Aggregator grouper = new Aggregator(
                "date", "update", "title", "url", "logo", "breadcrumb", "text", "lng", "domain", "children", "links", "branch"
        );

        Pipeline pipeline = new Pipeline();

        pipeline.add(Aggregates.match(filter));
        pipeline.add(Aggregates.limit(1));


        pipeline.add(Aggregates.lookup("BlobFiles", "logo", "_id", "logo"));

        pipeline.add(Aggregates.unwind("$logo", new UnwindOptions().preserveNullAndEmptyArrays(true)));

        pipeline.add(Aggregates.addFields(
                new Field<>("logo", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$logo._id", new BsonUndefined())), null,
                        new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$logo._id"))
                ))),
                new Field<>("domain", Pipeline.getDomainExpose("$lng"))
        ));


        pipeline.makeBreadcrumb();
        pipeline.add(makeUrl());

        pipeline.add(Aggregates.graphLookup("Pages", "$_id", "_id", "parents.id", "branch"));
        pipeline.add(Aggregates.addFields(new Field<>("branch", new Json("$concatArrays", Arrays.asList(
                                List.of("$_id"),
                                new Json("$map", new Json("input", "$branch").put("as", "ele").put("in", "$$ele._id"))
                        )))
                )
        );

        pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

        Json page = Db.aggregate("Pages", pipeline).first();

        if (page == null) {
            return null;
        }
        if (paging_str == null) {
            Json children_ = getPages(Filters.eq("parents", new Json("type", "Pages").put("id", page.getId())), 1000, null);
            if (children_ != null && children_.getListJson("result") != null) {
                List<Json> children = children_.getListJson("result");
                List<String> order = page.getList("children") == null ? new ArrayList<>() : page.getList("children");
                children.sort((that, other) -> {
                    int other_index = order.indexOf(other.getId());
                    int that_index = order.indexOf(that.getId());
                    String other_sort = other_index >= 0 ? String.valueOf(other_index) : other.getId();
                    String that_sort = that_index >= 0 ? String.valueOf(that_index) : that.getId();
                    return that_sort.compareTo(other_sort);
                });
                page.put("children", children);
            } else {
                page.put("children", new ArrayList<>());
            }
        }
        // Fx.log(page.getList("branch"));
        page.put("threads", ThreadsAggregator.getThreads(
                Filters.and(
                        Filters.ne("replies", 0),
                        Filters.in("parents.id", page.getList("branch"))
                ), paging_str, user));

        if (paging_str == null) {
            page.put("noreply", ThreadsAggregator.getThreads(
                    Filters.and(
                            Filters.eq("replies", 0),
                            Filters.in("parents.id", page.getList("branch"))
                    ), null, user));
        }
        page.remove("branch");

        return page;

    }


    public static Json getPages(Bson filter, int limit, String next_str) {

        Paginer paginer = new Paginer(next_str, "-update", limit);


        Aggregator grouper = new Aggregator(
                "date", "update", "title", "text", "url", "logo", "breadcrumb", "lng", "domain", "links", "branch"
        );

        Pipeline pipeline = new Pipeline();
        List<Bson> filters = new ArrayList<>();
        if (filter != null) {
            filters.add(filter);
        }

        Bson paging_filter = paginer.getFilters();
        if (paging_filter != null) {
            filters.add(paging_filter);
        }
        if (!filters.isEmpty()) {
            pipeline.add(Aggregates.match(Filters.and(filters)));
        }
        pipeline.add(paginer.getFirstSort());
        pipeline.add(paginer.getLimit());

        pipeline.add(Aggregates.lookup("BlobFiles", "logo", "_id", "logo"));

        pipeline.add(Aggregates.unwind("$logo", new UnwindOptions().preserveNullAndEmptyArrays(true)));

        pipeline.add(Aggregates.addFields(
                new Field<>("logo", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$logo._id", new BsonUndefined())), null,
                        new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$logo._id"))
                ))),
                new Field<>("domain", Pipeline.getDomainExpose("$lng")),
                new Field<>("text", new Json("$arrayElemAt", Arrays.asList(new Json("$split", Arrays.asList("$text", "\n")), 0)))
        ));


        pipeline.makeBreadcrumb();
        pipeline.add(makeUrl());


        pipeline.add(Aggregates.project(grouper.getProjectionOrder()));


        pipeline.add(paginer.getLastSort());


        return paginer.getResult("Pages", pipeline);
    }


    public static Json getThreads(String id, String paging_str, boolean noreply, Users user) {


        Pipeline pipeline = new Pipeline();

        pipeline.add(Aggregates.match(Filters.eq("_id", id)));
        pipeline.add(Aggregates.limit(1));


        pipeline.add(Aggregates.graphLookup("Pages", "$_id", "_id", "parents.id", "branch"));
        pipeline.add(Aggregates.project(new Json("branch", new Json("$concatArrays", Arrays.asList(
                List.of("$_id"),
                new Json("$map", new Json("input", "$branch").put("as", "ele").put("in", "$$ele._id"))))))
        );

        Json page = Db.aggregate("Pages", pipeline).first();
        if (page == null) {
            return null;
        }
        if (noreply) {
            return ThreadsAggregator.getThreads(
                    Filters.and(
                            Filters.eq("replies", 0),
                            Filters.in("parents.id", page.getList("branch"))
                    ), paging_str, user);
        }
        return ThreadsAggregator.getThreads(
                Filters.and(
                        Filters.ne("replies", 0),
                        Filters.in("parents.id", page.getList("branch"))
                ), paging_str, user);

    }


    public static Json getBase(String lng) {
        return PagesAggregator.getPages(Filters.and(Filters.ne("url", "copyright"), Filters.eq("lng", lng), Filters.eq("parents", new ArrayList<>())), 1000, null);
    }


    public static Bson makeUrl() {
        return Aggregates.addFields(new Field<>("url",
                new Json("$concat", List.of(
                        new Json("$cond", new Json()
                                .put("if", new Json("$eq", Arrays.asList(new Json("$size", AggregateUtils.forceArray("$breadcrumb")), 0)))
                                .put("then", "")
                                .put("else", new Json("$getField", new Json().put("field", "url")
                                        .put("input", new Json("$last", "$breadcrumb"))
                                ))),
                        "/",
                        new Json("$cond", new Json()
                                .put("if", new Json("$eq", Arrays.asList(new Json("$size", AggregateUtils.forceArray("$url")), 0)))
                                .put("then", "$_id")
                                .put("else", new Json("$arrayElemAt", Arrays.asList("$url", 0))
                                )))
                )

        ));
    }
}
