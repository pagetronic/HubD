/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.db.utils.paginer;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import live.page.hubd.system.Language;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.utils.Fx;
import live.page.hubd.system.utils.Hidder;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used for pagination of NoSQL request
 */
public class Paginer {

    protected final int limit;
    protected final String key;
    protected final int order;
    protected Direction direction = Direction.FIRST;
    protected Json paging = new Json();

    public Paginer(String paging_str, String sort_str, int limit) {

        this.limit = limit;
        this.key = sort_str.replaceAll("^-", "");
        this.order = sort_str.startsWith("-") ? -1 : 1;

        if (paging_str != null && (paging_str.equals("last") || paging_str.equals("first"))) {
            direction = paging_str.equals("last") ? Direction.LAST : Direction.FIRST;

        } else {
            paging = Hidder.decodeDecode(paging_str);

            if (paging != null && paging.get("@") != null) {
                direction = paging.getInteger("@", 1) == 1 ? Direction.FORWARD : Direction.REWIND;
            }
        }

    }

    /**
     * Generate pagination html string
     *
     * @param url       for base
     * @param pager     name of parameter to use in url
     * @param paging    object where find information about pagination
     * @param isfirst   is start at end or first ?
     * @param anchor    #anchor to jump after loading url
     * @param incontent add special string in the middle of the result
     * @param lng       lang to user
     * @return a string of html
     * @throws IOException is there a writer ?
     */
    public static String getHtml(String url, String pager, Json paging, boolean isfirst, String anchor, String incontent, String lng) throws IOException {

        if (url == null || pager == null) {
            return "";
        }
        StringWriter writer = new StringWriter();
        String base = url + (url.contains("?") ? "&amp;" : "?");

        String next = paging.getString("next");
        String prev = paging.getString("prev");

        if (prev == null) {
            writer.write("<em>" + Language.get("FAST_REWIND", lng) + "</em>");
            writer.write("<em><txt>" + Language.get("PREVIOUS", lng) + "</txt>" + "</em>");
        } else {
            writer.write("<a href=\"" + (base + (!isfirst ? pager + "=first" : "")).replaceAll("&amp;$", "").replaceAll("\\?$", "") + anchor + "\">" + Language.get("FAST_REWIND", lng) + "</a>");
            writer.write("<a href=\"" + base + pager + "=" + prev + anchor + "\">" + Language.get("PREVIOUS", lng) + "</a>");
        }
        if (incontent != null) {
            writer.write(incontent);
        }

        if (next == null) {
            writer.write("<em>" + "<txt>" + Language.get("NEXT", lng) + "</txt>" + "</em>");
            writer.write("<em>" + Language.get("FAST_FORWARD", lng) + "</em>");
        } else {
            writer.write("<a href=\"" + base + pager + "=" + next + anchor + "\">" + Language.get("NEXT", lng) + "</a>");
            writer.write("<a href=\"" + (base + (isfirst ? pager + "=last" : "")).replaceAll("&amp;$", "").replaceAll("\\?$", "") + anchor + "\">" + Language.get("FAST_FORWARD", lng) + "</a>");
        }
        writer.close();
        return writer.toString();
    }

    public int order() {
        return order;
    }

    public int getLimitInt() {
        return limit + (direction.equals(Direction.FIRST) || direction.equals(Direction.LAST) ? 1 : 2);
    }


    public Bson getLimit() {
        return Aggregates.limit(getLimitInt());
    }

    /**
     * First Sort to use, need when direction is "back"
     *
     * @return Aggregation object for sorting
     */
    public Bson getFirstSort() {
        return Aggregates.sort(
                ((direction.equals(Direction.REWIND) || direction.equals(Direction.LAST) ? -1 : 1) * order) > 0 ?
                        Sorts.orderBy(Sorts.ascending(key), Sorts.descending("_id")) :
                        Sorts.orderBy(Sorts.descending(key), Sorts.ascending("_id"))
        );
    }

    /**
     * Last Sort to use, the good direction
     *
     * @return Aggregation object for sorting
     */
    public Bson getLastSort() {
        return Aggregates.sort(order > 0 ? Sorts.orderBy(Sorts.ascending(key), Sorts.descending("_id")) : Sorts.orderBy(Sorts.descending(key), Sorts.ascending("_id")));
    }

    /**
     * Make filter depend on state
     *
     * @return Filter for query
     */
    public Bson getFilters() {
        if (paging == null) {
            return null;
        }

        Bson gt = Filters.or(Filters.gt(key, paging.get(key)), Filters.and(Filters.eq(key, paging.get(key)), Filters.lte("_id", paging.getId())));
        Bson lt = Filters.or(Filters.lt(key, paging.get(key)), Filters.and(Filters.eq(key, paging.get(key)), Filters.gte("_id", paging.getId())));

        if (direction.equals(Direction.FORWARD)) {
            return order > 0 ? gt : lt;
        }
        if (direction.equals(Direction.REWIND)) {
            return order > 0 ? lt : gt;
        }
        return null;
    }

    /**
     * Query DB and compose result with paging
     *
     * @param collection to query in DB
     * @param pipeline   for Aggregation
     * @return Special json include result and paging
     */
    public Json getResult(String collection, List<Bson> pipeline) {
        return getResult(collection, pipeline, null);
    }

    /**
     * Query DB and compose result with paging
     *
     * @param collection to query in DB
     * @param pipeline   for Aggregation
     * @return Special json include result and paging
     */
    public Json getResult(String collection, List<Bson> pipeline, String base) {
        try {
            List<Json> results = new ArrayList<>();
            try (MongoCursor<Json> result_it = Db.aggregate(collection, pipeline).iterator()) {
                while (result_it.hasNext()) {
                    results.add(result_it.next());
                }
            }
            Json result = getResult(results);
            Json paging = result.getJson("paging");
            if (paging != null) {
                paging.put("base", base);
                result.put("paging", paging);
            }
            return result;
        } catch (Exception e) {
            if (Fx.IS_DEBUG) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Compose result with paging on an already queried
     *
     * @param results already queried
     * @return special json include result and paging
     */
    public Json getResult(List<Json> results) {
        if (results == null || results.isEmpty()) {
            return new Json("result", new ArrayList<>()).put("paging", getPaging(null, null));
        }
        if (direction.equals(Direction.FIRST)) {
            return getFirst(results);
        }
        if (direction.equals(Direction.LAST)) {
            return getLast(results);
        }
        if (direction.equals(Direction.REWIND)) {
            return getRewind(results);
        }
        return getForward(results);
    }

    /**
     * Get the first truly result
     *
     * @param results already queried
     * @return the first object
     */
    private Json getFirst(List<Json> results) {
        int size = results.size();
        Json last = null;

        if (size > limit) {
            last = results.get(limit - 1);
        }

        return new Json("result", results.subList(0, Math.min(size, limit))).put("paging", getPaging(null, last));
    }

    /**
     * Get the last truly result
     *
     * @param results already queried
     * @return the first object
     */
    private Json getLast(List<Json> results) {
        int size = results.size();
        Json first = null;
        if (size > limit) {
            first = results.get(1);
        }
        return new Json("result", results.subList(Math.max(0, size - limit), size)).put("paging", getPaging(first, null));
    }

    /**
     * Get result on forward query
     *
     * @param results already queried
     * @return special json include result and paging
     */
    private Json getForward(List<Json> results) {
        int size = results.size();
        int start;
        int stop;

        Json first;
        Json last;

        if (size - 2 == limit) {
            start = 1;
            stop = size - 1;
            first = results.get(1);
            last = results.get(size - 2);

        } else if (size - 1 == limit) {
            start = 1;
            stop = size;
            first = results.get(0);
            last = null;
        } else {
            start = 0;
            stop = size;
            first = results.get(0);
            last = null;

            if (first.getId().equals(paging.getId())) {
                start = 1;
                if (size >= 2) {
                    first = results.get(1);
                } else {
                    first = null;
                }
            }
        }

        return new Json("result", results.subList(start, stop)).put("paging", getPaging(first, last));
    }

    /**
     * Get result on rewind query
     *
     * @param results already queried
     * @return special json include result and paging
     */
    private Json getRewind(List<Json> results) {

        int size = results.size();
        int start;
        int stop;
        Json prev;
        Json next;


        if (size - 2 == limit) {
            start = 1;
            stop = size - 1;
            prev = results.get(1);
            next = results.get(size - 2);

        } else if (size - 1 == limit) {
            start = 0;
            stop = results.get(size - 1).getId().equals(paging.getId()) ? size - 1 : size;
            next = results.get(stop - 1);
            prev = null;
        } else {
            prev = null;
            start = 0;
            stop = size;
            next = results.get(size - 1);
            if (next.getId().equals(paging.getId())) {
                stop = size - 1;
                if (size >= 2) {
                    next = results.get(size - 2);
                } else {
                    next = null;
                }
            }
        }


        return new Json("result", results.subList(start, stop)).put("paging", getPaging(prev, next));
    }

    /**
     * Generate pagination object aka paging
     *
     * @param first result
     * @param last  result
     * @return paging object
     */
    protected Json getPaging(Json first, Json last) {
        Json paging = new Json();
        try {
            if (first != null) {
                Object first_value = key.contains(".") ? first.getJson(key.split("\\.")[0]).get(key.split("\\.")[1]) : first.get(key);
                paging.put("prev", Hidder.encodeJson(new Json("@", -1).put(key, first_value).put("id", first.getId())));
            }
            if (last != null) {
                Object last_value = key.contains(".") ? last.getJson(key.split("\\.")[0]).get(key.split("\\.")[1]) : last.get(key);
                paging.put("next", Hidder.encodeJson(new Json("@", 1).put(key, last_value).put("id", last.getId())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        paging.put("limit", limit);
        return paging;
    }

    protected enum Direction {
        FIRST, LAST, REWIND, FORWARD
    }

}
