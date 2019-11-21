/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.socket;

import live.page.web.utils.json.Json;

public class SocketMessage {

	private Json datas = new Json();

	public SocketMessage() {

	}

	public SocketMessage(String channel) {
		datas.put("channel", channel);
	}

	public SocketMessage addMessage(String key, Object value) {

		Json data = datas.getJson("message");
		if (data == null) {
			data = new Json();
		}
		data.put(key, value);
		datas.put("message", data);
		return this;
	}

	public void setMessage(Json json) {
		datas.put("message", json);
	}

	public void setMessage(String key, Object value) {
		datas.put("message", new Json(key, value));

	}

	public boolean isEmpty() {
		return datas.isEmpty();
	}

	public void put(String key, Object value) {
		datas.put(key, value);
	}

	public Object getMessage() {
		return datas.get("message");
	}

	@Override
	public String toString() {
		return datas.toString();
	}
}
