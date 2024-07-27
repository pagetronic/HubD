/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.socket;

import live.page.hubd.system.json.Json;

public class SocketMessage {

    private final Json datas = new Json();

    public SocketMessage() {

    }

    public SocketMessage(String channel) {
        datas.put("channel", channel);
    }

    public SocketMessage put(String key, Object value) {

        Json data = datas.getJson("message");
        if (data == null) {
            data = new Json();
        }
        data.put(key, value);
        datas.put("message", data);
        return this;
    }

    public boolean isEmpty() {
        return datas.isEmpty();
    }


    public Json getMessage() {
        return datas.getJson("message");
    }

    public SocketMessage setMessage(Json json) {
        datas.put("message", json);
        return this;
    }

    @Override
    public String toString() {
        return datas.toString();
    }
}
