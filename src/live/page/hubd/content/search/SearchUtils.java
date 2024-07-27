/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.search;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.PipelinerStore;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.db.utils.paginer.PolyPaginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.utils.Fx;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SearchUtils {
    private static final int limit = 20;

    public static Json search(String query, String lng, String type, String paging_str) {
        try {
            if (type != null && !type.isEmpty()) {
                if (!PipelinerStore.getMethods().containsKey(type)) {
                    return null;
                }
                return SearchUtils.searchOne(query, lng, type, paging_str);
            }
            return SearchUtils.searchAll(query, lng, paging_str, PipelinerStore.getMethods().keySet());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Json searchAll(String query, String lng, String paging_str, Set<String> keys) throws Exception {

        PolyPaginer paginer = new PolyPaginer(paging_str, "-score", limit, keys);

        Pipeline pipeline = new Pipeline();

        pipeline.add(Aggregates.limit(1));

        pipeline.add(Aggregates.project(new Json("_id", false).put("empty", true)));

        for (String collection : PipelinerStore.getMethods().keySet()) {

            PipelinerStore.PipelineMaker searcher = PipelinerStore.getMethods().get(collection).getConstructor(String.class, String.class, Paginer.class).newInstance(collection, lng, paginer);
            pipeline.add(Aggregates.lookup(Fx.ucfirst(collection), searcher
                            .addFilter(Filters.and(Filters.exists("remove", false), Filters.text(query)))
                            .getSearcher(),
                    collection));
        }


        List<String> keys_exps = new ArrayList<>();
        for (String key : PipelinerStore.getMethods().keySet()) {
            keys_exps.add("$" + key);
        }
        pipeline.add(Aggregates.project(new Json("result", new Json("$concatArrays", keys_exps))));

        pipeline.add(Aggregates.unwind("$result"));
        pipeline.add(Aggregates.replaceRoot("$result"));

        pipeline.add(paginer.getLastSort());

        pipeline.add(paginer.getLimit());

        return paginer.getResult("Users", pipeline);

    }

    private static Json searchOne(String query, String lng, String collection, String paging_str) throws Exception {
        Paginer paginer = new Paginer(paging_str, "-score", limit);
        PipelinerStore.PipelineMaker searcher = PipelinerStore.getMethods().get(collection).getConstructor(String.class, String.class, Paginer.class).newInstance(collection, lng, paginer);
        return paginer.getResult(Fx.ucfirst(collection), searcher.addFilter(Filters.text(query)).setLng(lng).getSearcher());
    }


    public static String cleanQuery(String query) {
        if (query == null) {
            return null;
        }
        query = Normalizer.normalize(query, Normalizer.Form.NFKC).replaceAll("(</?[^>]+>)", "");
        return Fx.truncate(query, 150);
    }
}
