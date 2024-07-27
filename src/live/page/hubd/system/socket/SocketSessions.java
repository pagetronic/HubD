/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.socket;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.Session;
import live.page.hubd.utils.Fx;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@WebListener
public class SocketSessions implements ServletContextListener {

    private final static Map<String, SessionData> sessions = new HashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> future = null;

    public static Map<String, SessionData> getSessions() {
        return sessions;
    }

    public static SessionData get(String id) {
        return sessions.get(id);
    }

    public static void put(String id, SessionData data) {
        sessions.put(id, data);
    }

    public static void remove(String id) {
        sessions.remove(id);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        future = scheduler.scheduleAtFixedRate(() -> {
            ByteBuffer ping = ByteBuffer.wrap("alive…?¿".getBytes());
            for (Map.Entry<String, SessionData> entry : sessions.entrySet()) {
                SessionData session_data = entry.getValue();
                Session session = session_data.getSession();
                try {
                    session.getBasicRemote().sendPing(ping);
                } catch (Exception e) {
                    try {
                        session.close();
                    } catch (Exception ignore) {
                    }
                    sessions.remove(entry.getKey());
                }
            }
        }, 45, 45, TimeUnit.SECONDS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (future != null) {
            future.cancel(true);
        }
        Fx.shutdownService(scheduler);
        sessions.clear();
    }
}
