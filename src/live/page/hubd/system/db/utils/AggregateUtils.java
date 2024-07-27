package live.page.hubd.system.db.utils;

import com.mongodb.client.model.Filters;
import live.page.hubd.system.json.Json;
import org.bson.BsonUndefined;

import java.util.ArrayList;
import java.util.Arrays;

public class AggregateUtils {

    public static Json forceArray(String arr) {
        return new Json("$cond", new Json()
                .put("if", new Json("$isArray", arr))
                .put("then", arr)
                .put("else", new ArrayList<>()));
    }

    public static Json arrayElemAt(String key, int index) {
        return new Json("$cond", Arrays.asList(
                Filters.and(
                        new Json("$ne", Arrays.asList(key, new BsonUndefined())),
                        new Json("$ne", Arrays.asList(key, null)),
                        new Json("$isArray", key),
                        new Json("$gt", Arrays.asList(new Json("$size", key), 0))
                ),
                new Json("$arrayElemAt", Arrays.asList(key, index)),
                null
        ));

    }
}
