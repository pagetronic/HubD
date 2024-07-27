/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.sessions;

import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.json.Json;

import java.util.Date;

public abstract class UsersBase {

    public static Json getBase() {
        Date date = new Date();
        return new Json("coins", 0).put("cash", 0).put("join", date).put("last", date);

    }

    public static Aggregator getGrouper() {

        return new Aggregator("name", "join", "coins", "cash", "posts", "avatar", "last", "groups", "settings", "country");

    }

}
