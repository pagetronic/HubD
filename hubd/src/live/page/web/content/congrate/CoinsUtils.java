/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.congrate;

import com.mongodb.client.model.Filters;
import live.page.web.system.db.Db;
import live.page.web.system.db.tags.DbTags;
import live.page.web.system.json.Json;
import live.page.web.system.socket.SocketPusher;

import java.util.Date;

public class CoinsUtils {

	public static final int THREAD = 2;
	public static final int POST = 1;

	public static Json congrate(String user_id, String element) {

		Json user = Db.findById("Users", user_id);
		if (user == null) {
			return new Json("error", "PLEASE_LOGIN");
		}

		int user_coins = user.getInteger("coins", 0);

		if (user_coins <= 0) {
			return new Json("error", "NO_COINS");
		}
		DbTags ele = new DbTags(element);
		if (ele.getId() == null) {
			return new Json("error", "UNKNOWN_ELEMENT");
		}

		Json congrated = Db.findOneAndUpdate(ele.getCollection(), Filters.eq("_id", ele.getId()),
				new Json("$push", new Json("coins", new Json("user", user_id).put("date", new Date()))));
		if (congrated == null) {
			return new Json("error", "UNKNOWN_ELEMENT");
		} else {
			if (congrated.getString("user") != null) {
				coinsUser(1, congrated.getString("user"));
			}
			coinsUser(-1, user_id);
			return new Json("coins", congrated.getListJson("coins").size());
		}
	}


	public static void coinsUser(int inc, String user_id) {

		Json user = Db.findOneAndUpdate("Users", Filters.eq("_id", user_id), new Json("$inc", new Json("coins", inc)));
		if (user != null) {
			SocketPusher.send("user", user_id, new Json("action", "coins").put("coins", user.getInteger("coins")));
		}
	}

}
