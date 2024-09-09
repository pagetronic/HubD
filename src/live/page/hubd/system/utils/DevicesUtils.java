package live.page.hubd.system.utils;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;

import java.util.Date;

public class DevicesUtils {

    public static Json uuid(Users user, Json device) {
        if (user == null || device == null) {
            return null;
        }
        device.sort();
        Json rez = Db.findOneAndUpdate("Devices",
                Filters.and(
                        Filters.eq("user", user.getId()),
                        Filters.eq("device", device)
                ),
                new Json()
                        .put("$set", new Json()
                                .put("update", new Date())
                        )
                        .put("$setOnInsert", new Json()
                                .put("_id", Fx.getSecureKey())
                                .put("user", user.getId())
                                .put("date", new Date())
                                .put("device", device)
                        ),
                new FindOneAndUpdateOptions().sort(Sorts.descending("date")).returnDocument(ReturnDocument.AFTER).upsert(true));
        return new Json("uuid", rez.getId());
    }
}
