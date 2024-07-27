/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.profile;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;

import java.util.List;

public class ProfileUtils {

    public static Json setSettings(String user_id, Json data) {

        if (data.isEmpty()) {
            return Db.findOneAndUpdate("Users", Filters.eq("_id", user_id), new Json("$set", new Json("settings", new Json())), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)).getJson("settings");
        }

        Json addToSet = new Json();
        Json set = new Json();
        Json unset = new Json();
        Json update = new Json();
        for (String key : data.keySet()) {
            if (data.get(key) == null) {
                unset.put("settings." + key, "");
            } else if (data.get(key).getClass().isAssignableFrom(List.class)) {
                addToSet.put("settings." + key, new Json("$each", data.get(key)));
            } else {
                set.put("settings." + key, data.get(key));
            }
        }
        if (!addToSet.isEmpty()) {
            update.put("$addToSet", addToSet);
        }
        if (!set.isEmpty()) {
            update.put("$set", set);
        }
        if (!unset.isEmpty()) {
            update.put("$unset", unset);
        }
        return Db.findOneAndUpdate("Users", Filters.eq("_id", user_id), update, new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)).getJson("settings");
    }

}
