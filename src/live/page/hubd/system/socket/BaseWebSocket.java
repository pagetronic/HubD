/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.socket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import live.page.hubd.content.profile.ProfileUtils;
import live.page.hubd.system.Settings;
import live.page.hubd.system.StatsTools;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.system.utils.Fx;
import live.page.hubd.system.utils.ai.IAUtils;

import java.io.IOException;
import java.util.Map;


@ServerEndpoint(value = "/socket", configurator = SocketConfig.class)
public class BaseWebSocket {

    public SocketMessage onMessageAuthSys(Json msg, SessionData sessiondata) {

        switch (msg.getString("action")) {
            case "settings" -> {
                SocketPusher.send("user", sessiondata.getUser().getId(), new Json("action", "settings").put("settings", ProfileUtils.setSettings(sessiondata.getUser().getId(), msg.getJson("data"))));
                return new SocketMessage(msg.getString("act")).put("ok", true);
            }
            case "ai" -> {
                Users user = sessiondata.getUser();
                if (user.getAdmin()) {
                    return IAUtils.socket(msg, user);
                }
            }
        }
        return null;
    }

    public SocketMessage onMessagePublicSys(Json msg, SessionData sessiondata) {

        SocketMessage data = new SocketMessage(msg.getString("act"));
        switch (msg.getString("action")) {
            case "stats" -> {
                return StatsTools.pushStats(msg.getString("act"), sessiondata.getIp(), msg.getJson("data"));
            }
            case "follow" -> {
                sessiondata.follow(msg.getString("channel"));
                if (msg.getString("channel").equals("user")) {
                    Users user = sessiondata.getUser();
                    if (user != null) {
                        SocketPusher.sendNoticesCount(user.getId());
                    }
                }
                return data;

            }
            case "abort" -> {
                sessiondata.abort(msg.getString("act"));
                return new SocketMessage();
            }
            case "unfollow" -> {
                sessiondata.unfollow(msg.getString("channel"));
                return data;
            }
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

            if (msg.getString("action") == null) {
                return;
            }

            SocketMessage data = onMessagePublicSys(msg, user_session);

            if (data == null && user_session.getUser() == null) {
                data = new SocketMessage(msg.getString("act"));
                data.put("error", "PLEASE_LOGIN");
            } else if (data == null && user_session.getUser() != null) {
                data = onMessageAuthSys(msg, user_session);
            }


            if (data == null) {
                data = new SocketMessage(msg.getString("act"));
                data.put("error", "UNKNOWN_METHOD");
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

            if (!Fx.IS_DEBUG) {
                String origine = (String) session.getUserProperties().get("origin");
                if (!session.isSecure() || (origine != null && (!origine.endsWith(Settings.STANDARD_HOST) && !origine.startsWith("http://localhost:")))) {
                    session.close();
                    return;
                }
            }

            Map<String, Object> prop = session.getUserProperties();

            if ((boolean) prop.getOrDefault("session_error", false)) {
                session.getBasicRemote().sendText("PLEASE_LOGIN");
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "PLEASE_LOGIN"));
                return;
            }


            SessionData sessiondata = new SessionData(session);
            sessiondata.setUser((Json) session.getUserProperties().get("user"));
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
