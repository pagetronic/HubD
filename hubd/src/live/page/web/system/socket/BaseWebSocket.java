/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.socket;

import live.page.web.admin.utils.scrap.ScrapAdminUtils;
import live.page.web.content.messages.MessagesUtils;
import live.page.web.content.notices.NoticesUtils;
import live.page.web.content.profile.ProfileUtils;
import live.page.web.system.Settings;
import live.page.web.system.StatsTools;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;


@ServerEndpoint(value = "/socket", configurator = SocketConfig.class)
public class BaseWebSocket {

	public SocketMessage onMessageAuthSys(Json msg, SessionData sessiondata) {
		switch (msg.getString("action")) {
			case "scrap":
				return ScrapAdminUtils.scrapPreview(msg, sessiondata);
			case "receive_notices":
				NoticesUtils.noticeReceived(sessiondata.getUserId());
				return new SocketMessage();
			case "read_message":
				MessagesUtils.readMessage(sessiondata.getUserId(), msg.getId(), msg.getBoolean("read", false));
				return new SocketMessage(msg.getString("act")).putKeyMessage("ok", true);
			case "settings":
				SocketPusher.send("user", sessiondata.getUserId(), new Json("action", "settings").put("settings", ProfileUtils.setSettings(sessiondata.getUserId(), msg.getJson("data"))));
				return new SocketMessage(msg.getString("act")).putKeyMessage("ok", true);

		}
		return null;
	}

	public SocketMessage onMessagePublicSys(Json msg, SessionData sessiondata) {

		SocketMessage data = new SocketMessage(msg.getString("act"));
		switch (msg.getString("action")) {
			case "stats":
				return StatsTools.pushStats(msg.getString("act"), sessiondata.getIp(), msg.getJson("data"));

			case "follow":
				sessiondata.addElement(msg.getString("channel"));
				if (msg.getString("channel").equals("user")) {
					String user_id = sessiondata.getUserId();
					SocketPusher.sendNoticesCount(user_id);
					MessagesUtils.pushCount(user_id);
				}
				if (msg.getString("channel").equals("stats")) {
					data.setMessage(new Json("live", StatsTools.getLive()));
				}
				return data;
			case "abort":
				sessiondata.abort(msg.getString("act"));
				return new SocketMessage();
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
				data.putKeyMessage("error", "PLEASE_LOGIN");
			} else if (data == null && user_session.getUserId() != null) {
				data = onMessageAuthSys(msg, user_session);
			}
			if (data == null) {
				data = new SocketMessage(msg.getString("act"));
				data.putKeyMessage("error", "UNKNOWN_METHOD");
			}
			try {
				if (data.getMessage() != null) {
					if (session.isOpen()) {
						session.getAsyncRemote().sendText(data.toString());
					}
				}
			} catch (Exception ignore) {
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
			if (!session.isSecure() || (origine != null && !origine.endsWith(Settings.STANDARD_HOST)) || !session.getUserProperties().containsKey("hl")) {
				session.close();
				return;
			}

			SessionData sessiondata = new SessionData(session);
			sessiondata.setUserId((String) session.getUserProperties().get("user_id"));
			sessiondata.setLang((String) session.getUserProperties().get("hl"));
			sessiondata.setIp((String) session.getUserProperties().get("ip"));
			SocketSessions.put(session.getId(), sessiondata);

		} catch (Exception e) {
			try {
				session.close();
			} catch (Exception ignore) {
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
			}
		} catch (Exception ignore) {

		}
	}
}
