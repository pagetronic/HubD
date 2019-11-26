/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.socket;

import com.mongodb.client.model.Filters;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SocketPusher {

	public static void sendData(String user_id, String action, Object data) {
		Json notif = new Json();
		notif.put("action", action);
		notif.put("data", data);
		send("user", Arrays.asList(user_id), notif);
	}

	public static void send(String channel, Object message) {
		send(channel, Arrays.asList(), message);
	}

	public static void send(String channel, String user, Object message) {
		send(channel, Arrays.asList(user), message);
	}

	public static void send(String channel, List<String> users, final Object message) {
		if (message == null) {
			return;
		}
		Json push = new Json("channel", channel).put("message", message).put("date", new Date());
		if (users != null && users.size() > 0) {
			push.put("users", users);
		}
		Db.save("Push", push);
	}

	public static void sendNoticesCount(String user_id) {

		send("user", user_id, new Json("action", "notices").put("notices", countUnreads(user_id)));
	}

	public static String countUnreads(String user_id) {

		int counts = (int) Db.countLimit("Notices", Filters.and(Filters.eq("user", user_id), Filters.exists("read", false)), 100);
		return counts >= 100 ? counts + "+" : counts + "";
	}
}
