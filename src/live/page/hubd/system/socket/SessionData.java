/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.socket;

import jakarta.websocket.Session;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SessionData {

    private final Session session;
    private final Date date = new Date();
    private final List<String> channels = new ArrayList<>();
    private final List<String> aborts = new ArrayList<>();
    private Json user = null;
    private String ip = null;
    private String lng = null;
    private StringWriter temp_message = new StringWriter();

    public SessionData(Session session) {
        this.session = session;

    }

    public Date getDate() {
        return date;
    }

    public Session getSession() {
        return session;
    }

    public Users getUser() {
        return user == null ? null : new Users(user);
    }

    public void setUser(Json user) {
        this.user = user;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void follow(String channel) {
        if (!channels.contains(channel)) {
            channels.add(channel);
        }
    }

    public void unfollow(String channel) {
        channels.remove(channel);
    }

    public void addMsg(String message) {
        temp_message.append(message);
    }

    public void send(SocketMessage message) {
        synchronized (session) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getMsg() {
        try {
            return temp_message.toString();
        } finally {
            clearMsg();
        }
    }

    public void clearMsg() {
        try {
            temp_message.close();
        } catch (Exception ignore) {
        }
        temp_message = new StringWriter();
    }

    public String getLang() {
        return lng;
    }

    public void setLang(String lng) {
        this.lng = lng;
    }

    public boolean isOpen() {
        return session.isOpen();
    }

    public void abort(String act) {
        aborts.add(act);
    }

    public boolean isAbort(String act) {
        try {
            return aborts.contains(act);
        } finally {
            aborts.remove(act);
        }
    }

}
