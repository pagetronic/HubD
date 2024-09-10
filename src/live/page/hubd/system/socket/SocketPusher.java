/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.socket;

import live.page.hubd.content.notices.NoticesUtils;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;

import java.util.Date;
import java.util.List;

public class SocketPusher {

    public static void sendData(String user_id, String action, Object data) {
        send("user", List.of(user_id), new Json().put("action", action).put("data", data));
    }

    public static void send(String channel, Object message) {
        send(channel, List.of(), message);
    }

    public static void send(String channel, String user, Object message) {
        send(channel, user == null ? null : List.of(user), message);
    }

    public static void send(String channel, List<String> users, final Object message) {
        if (message == null) {
            return;
        }
        Json push = new Json("channel", channel).put("message", message).put("date", new Date());
        if (users != null && !users.isEmpty()) {
            push.put("users", users);
        }
        Db.save("Push", push);
    }

    public static void sendNoticesCount(String user_id) {

        send("user", user_id, new Json("action", "notices").put("notices", NoticesUtils.countNotices(user_id)));
    }

}
