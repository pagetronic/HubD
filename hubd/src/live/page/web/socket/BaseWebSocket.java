/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.socket;

import live.page.web.admin.scrap.ScrapAdminUtils;
import live.page.web.messages.MessagesUtils;
import live.page.web.notices.NoticesUtils;
import live.page.web.profile.ProfileUtils;
import live.page.web.servlet.utils.LogsUtils;
import live.page.web.utils.Fx;
import live.page.web.utils.Settings;
import live.page.web.utils.json.Json;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;


@ServerEndpoint(value = "/socket", configurator = SocketConfig.class)
public class BaseWebSocket {

	public SocketMessage onMessageAuthSys(Json msg, SessionData user_session) {
		switch (msg.getString("action")) {
			case "abort":
				user_session.abort(msg.getString("act"));
				return new SocketMessage();
			case "scrap":
				return ScrapAdminUtils.scrapPreview(msg, user_session);
			case "stats":
				LogsUtils.pushStats(msg.getJson("data"));
				return new SocketMessage();
			case "receive_notices":
				NoticesUtils.noticeReceived(user_session.getUserId());
				return new SocketMessage();
			case "read_message":
				MessagesUtils.readMessage(user_session.getUserId(), msg.getId(), msg.getBoolean("read", false));
				return new SocketMessage(msg.getString("act")).addMessage("ok", true);
			case "settings":
				SocketPusher.send("user", user_session.getUserId(), new Json("action", "settings").put("settings", ProfileUtils.setSettings(user_session.getUserId(), msg.getJson("data"))));
				return new SocketMessage(msg.getString("act")).addMessage("ok", true);

		}
		return null;
	}

	public SocketMessage onMessagePublicSys(Json msg, SessionData sessiondata) {

		SocketMessage data = new SocketMessage(msg.getString("act"));
		switch (msg.getString("action")) {
			case "follow":
				sessiondata.addElement(msg.getString("channel"));

				if (msg.getString("channel").equals("user")) {
					String user_id = sessiondata.getUserId();
					SocketPusher.sendNoticesCount(user_id);
					MessagesUtils.pushCount(user_id);
				}

				return data;
			case "unfollow":
				sessiondata.removeElement(msg.getString("channel"));
				return data;
		}
		return null;
	}

	@OnMessage
	public void onMessage(String message, boolean isLast, Session session) throws IOException {

		SessionData user_session = SocketSessions.get(session.getId());
		try {
			if (user_session == null) {
				return;
			}
			user_session.addMsg(message);
			if (!isLast) {
				return;
			}
			Json msg = new Json(user_session.getMsg());
			user_session.clearMsg();

			if (msg == null || msg.getString("action") == null) {
				return;
			}

			SocketMessage data = onMessagePublicSys(msg, user_session);

			if (data == null && user_session.getUserId() == null) {
				data = new SocketMessage(msg.getString("act"));
				data.addMessage("error", "PLEASE_LOGIN");
			} else if (data == null && user_session.getUserId() != null) {
				data = onMessageAuthSys(msg, user_session);
			}
			if (data == null) {
				data = new SocketMessage(msg.getString("act"));
				data.addMessage("error", "UNKNOWN_METHOD");
			}
			try {
				if (data.getMessage() != null) {
					if (session.isOpen()) {
						session.getAsyncRemote().sendText(data.toString());
					}
				}
			} catch (Exception e) {
			}
		} catch (Exception e) {
			if (Fx.IS_DEBUG) {
				e.printStackTrace();
			}
			user_session.clearMsg();
		}
	}

	@OnOpen
	public void onOpen(Session session, EndpointConfig config) throws IOException {

		try {
			String origine = (String) session.getUserProperties().get("origin");
			if (!session.isSecure() || (origine != null && !origine.endsWith(Settings.HOST_HTTP)) || !session.getUserProperties().containsKey("hl")) {
				session.close();
				return;
			}

			SessionData sessiondata = new SessionData(session);
			sessiondata.setUserId((String) session.getUserProperties().get("user_id"));
			sessiondata.setLang((String) session.getUserProperties().get("hl"));
			SocketSessions.put(session.getId(), sessiondata);

		} catch (Exception e) {
			try {
				session.close();
			} catch (Exception ex) {
			}
		}
	}

	@OnClose
	public void onClose(Session session) {
		try {
			SocketSessions.remove(session.getId());
		} catch (Exception ignore) {

		}
	}

	@OnError
	public void onError(Session session, Throwable t) {
		try {
			if (session != null) {
				if (session.getId() != null) {
					SessionData session_data = SocketSessions.get(session.getId());
					if (session_data != null) {
						session_data.clearMsg();
						SocketSessions.remove(session.getId());
					}
				}
				if (session.isOpen()) {
					session.close();
				}
			}
		} catch (Exception e) {

		}
	}
}
