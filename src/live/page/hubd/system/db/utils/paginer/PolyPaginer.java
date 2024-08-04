/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.db.utils.paginer;

import com.mongodb.client.model.Filters;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.utils.Fx;
import live.page.hubd.system.utils.Hidder;
import org.bson.conversions.Bson;

import java.util.*;

/**
 * A Paginer.class used in Search result with multiple collections and methods
 * //TODO known as buggy
 */
public class PolyPaginer extends Paginer {

    private final Set<String> keys;

    public PolyPaginer(String paging_str, String sort_str, int limit, Set<String> keys) {
        super(paging_str, sort_str, limit);
        this.keys = keys;
    }

    /**
     * Make filter depend on state
     *
     * @param type of the filter needed
     * @return Filter for query
     */
    public Bson getFilters(String type) {
        if (paging == null || !paging.containsKey(type) || paging.getJson(type).get(key) == null) {
            return null;
        }
        Json paging_type = paging.getJson(type);
        if (paging_type == null) {
            return null;
        }
        Bson gt = Filters.or(Filters.gt(key, paging_type.get(key)), Filters.and(Filters.eq(key, paging_type.get(key)), Filters.lte("_id", paging_type.getId())));
        Bson lt = Filters.or(Filters.lt(key, paging_type.get(key)), Filters.and(Filters.eq(key, paging_type.get(key)), Filters.gte("_id", paging_type.getId())));

        if (direction.equals(Direction.FORWARD)) {
            return order > 0 ? gt : lt;
        }
        if (direction.equals(Direction.REWIND)) {
            return order > 0 ? lt : gt;
        }
        return null;

    }

    /**
     * Compose result with paging on an already queried
     *
     * @param results already queried
     * @return special json include result and paging
     */
    @Override
    public Json getResult(List<Json> results) {

        Map<String, List<Json>> results_map = new HashMap<>();
        for (Json result : results) {
            String type = result.getString("type", null);
            if (type == null) {
                Fx.log("Paginer have no Types");
                return null;
            }
            if (!results_map.containsKey(type)) {
                results_map.put(type, new ArrayList<>());
            }
            results_map.get(type).add(result);

        }
        Json next = new Json();
        // impossible (very impossible) to get previous because of no limit on end of first !!@#! you can't !
        Json prev = new Json();
        for (String key : results_map.keySet()) {
            Json paging = super.getResult(results_map.get(key)).getJson("paging");
            if (paging.containsKey("next")) {
                next.put(key, paging.getJson("next").remove("@"));
            }
            if (paging.containsKey("prev")) {
                prev.put(key, paging.getJson("prev").remove("@"));
            }

        }

        Json rez = super.getResult(results);
        List<Json> results_more = rez.getListJson("result");
        results_map = new HashMap<>();
        for (Json result : results_more) {
            String type = result.getString("type", null);
            if (type == null) {
                Fx.log("Paginer have no Types");
                return null;
            }
            if (!results_map.containsKey(type)) {
                results_map.put(type, new ArrayList<>());
            }
            results_map.get(type).add(result);

        }
        for (String key : results_map.keySet()) {
            if (!prev.containsKey(key) || !next.containsKey(key)) {
                List<Json> results_key = results_map.get(key);
                if (results_key.size() > 0) {
                    if (!prev.containsKey(key)) {
                        prev.put(key, makePaging(results_key.get(0)));
                    }
                    if (!next.containsKey(key)) {
                        if (results_key.size() > 1) {
                            next.put(key, makePaging(results_key.get(results_key.size() - 1)));
                        }
                    }
                }
            }
        }
        if (results_more.size() > 0) {
            for (String key : keys) {
                if (!next.containsKey(key) && results_more.size() > 1) {
                    next.put(key, makeExtraPaging(results_more.get(results_more.size() - 1)));
                }
                if (!prev.containsKey(key)) {
                    prev.put(key, makeExtraPaging(results_more.get(0)));
                }
            }
        }

        Json paging_ = rez.getJson("paging");
        if (paging_.containsKey("next")) {
            paging_.put("next", Hidder.encodeJson(next.prepend("@", paging_.getJson("next").getInteger("@"))));
        }
        if (paging_.containsKey("prev")) {
            paging_.put("prev", Hidder.encodeJson(prev.prepend("@", paging_.getJson("prev").getInteger("@"))));
        }

        paging_.put("prev", null);
        return rez.put("paging", paging_);


    }

    private Json makePaging(Json result) {
        Object value = key.contains(".") ? result.getJson(key.split("\\.")[0]).get(key.split("\\.")[1]) : result.get(key);
        return new Json(key, value).put("id", result.getId());
    }

    private Json makeExtraPaging(Json result) {
        Object value = key.contains(".") ? result.getJson(key.split("\\.")[0]).get(key.split("\\.")[1]) : result.get(key);
        return new Json(key, value);
    }

    /**
     * Generate pagination object aka paging
     *
     * @param first result
     * @param last  result
     * @return paging object
     */
    @Override
    protected Json getPaging(Json first, Json last) {
        Json paging = new Json();
        try {
            if (first != null) {
                Object first_value = key.contains(".") ? first.getJson(key.split("\\.")[0]).get(key.split("\\.")[1]) : first.get(key);
                paging.put("prev", new Json("@", -1).put(key, first_value).put("id", first.getId()));
            }
            if (last != null) {
                Object last_value = key.contains(".") ? last.getJson(key.split("\\.")[0]).get(key.split("\\.")[1]) : last.get(key);
                paging.put("next", new Json("@", 1).put(key, last_value).put("id", last.getId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        paging.put("limit", limit);
        return paging;
    }

}
