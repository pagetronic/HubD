/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.socket;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import live.page.hubd.system.Settings;
import live.page.hubd.system.sessions.BaseSession;
import live.page.hubd.system.sessions.Users;

import java.util.List;
import java.util.Map;

public class SocketConfig extends ServerEndpointConfig.Configurator {

    @Override
    public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
        return super.getNegotiatedSubprotocol(Settings.getLangs(), requested);
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {

        String ip = request.getParameterMap().get("ip").get(0);
        sec.getUserProperties().put("ip", ip);

        Map<String, List<String>> headers = request.getHeaders();

        sec.getUserProperties().put("host", headers.get("host").get(0));
        if (headers.get("origin") != null) {
            sec.getUserProperties().put("origin", headers.get("origin").get(0));
        }
        if (headers.get("user-agent") != null) {
            sec.getUserProperties().put("user-agent", headers.get("user-agent").get(0));
        }

        String session_id = null;
        List<String> cookies = headers.get("cookie");
        if (cookies != null && !cookies.isEmpty()) {

            String[] cookies_ = cookies.get(0).split("; ");
            for (String element : cookies_) {
                if (element.split("=")[0].equals(Settings.getCookieName())) {
                    session_id = element.split("=")[1];
                }
                if (element.split("=")[0].equals("tz")) {
                    sec.getUserProperties().put("tz", element.split("=")[1]);
                }
            }

        } else {
            List<String> authorization = headers.get("Authorization");
            if (authorization != null && !authorization.isEmpty()) {
                session_id = authorization.get(0);
            }
        }
        if (session_id == null && !sec.getSubprotocols().isEmpty()) {
            session_id = sec.getSubprotocols().get(0);
        }
        if (session_id != null) {
            Users user = BaseSession.getUser(ip, session_id);

            if (user != null) {
                sec.getUserProperties().put("user_id", user.getId());
                sec.getUserProperties().put("user", user);
            } else {
                sec.getUserProperties().put("session_error", true);
            }

        }
        /*
                List<String> swp = headers.get("sec-websocket-protocol");
                if (swp != null && swp.size() > 0 && langs.contains(swp.get(0))) {
                    String lng = headers.get("sec-websocket-protocol").get(0);
                    sec.getUserProperties().put("hl", lng);
                }

        */
    }
}
