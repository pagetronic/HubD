/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.socket;

import com.mongodb.CursorType;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Sorts;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import live.page.hubd.system.Language;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.utils.Fx;
import org.bson.Document;

import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * For multi-server system, you can't have a live websocket update if you don't store update in a common system to all servers
 * <p>
 * Here we use Capped Collection from MongodDB https://docs.mongodb.com/manual/core/capped-collections/
 * and Tailable Cursor https://docs.mongodb.com/manual/core/tailable-cursors/
 * <p>
 * Every push are live.
 * In Javascript use : socket.follow('channel/tofollow', function (msg) {});
 */
@WebListener
public class SocketCapped implements ServletContextListener {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Create and Run Capped Collection
     *
     * @param sce not used
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        executor.submit(() -> {
            Thread.currentThread().setName("socket-push");
            try {
                Date last_date = new Date();

                MongoDatabase db = Db.getDb();
                MongoCollection<Document> pushcol = db.getCollection("Push");
                pushcol.drop();
                CreateCollectionOptions options = new CreateCollectionOptions();
                options.capped(true);
                options.sizeInBytes(524288000);
                db.createCollection("Push", options);
                pushcol = db.getCollection("Push");
                pushcol.createIndex(Sorts.descending("date"), new IndexOptions().name("date"));
                Db.save("Push", new Json("date", new Date(last_date.getTime() - 1L)).put("seed", true));

                while (!executor.isShutdown() && !executor.isTerminated()) {
                    MongoCursor<Json> pushs = Db.getDb("Push").find(Filters.gt("date", last_date)).sort(Sorts.ascending("$natural")).cursorType(CursorType.TailableAwait).iterator();
                    while (pushs.hasNext() && !executor.isShutdown() && !executor.isTerminated()) {
                        try {
                            Json push = pushs.next();
                            if (push.containsKey("users")) {
                                pushToUser(push.getString("channel"), push.getList("users"), push.getJson("message"));
                            } else {
                                pushToAll(push.getString("channel"), push.getJson("message"), push.getList("excludes"));
                            }
                            last_date = push.getDate("date");
                        } catch (Exception ignored) {
                        }
                    }
                    pushs.close();
                }
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Fx.shutdownService(executor);
    }

    /**
     * Push to all users
     *
     * @param message  to push
     * @param excludes users
     */
    private void pushToAll(String channel, Json message, List<String> excludes) {
        for (Entry<String, SessionData> entry : SocketSessions.getSessions().entrySet()) {
            SessionData sessionData = entry.getValue();

            if ((excludes == null || !excludes.contains(sessionData.getUser())) && sessionData.getChannels().contains(channel)) {
                try {
                    SocketMessage data = new SocketMessage(channel);
                    data.setMessage(message);
                    sessionData.getSession().getAsyncRemote().sendText(data.toString());
                } catch (Exception e) {
                    if (Fx.IS_DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Push to a specifics users
     *
     * @param channel where push
     * @param users   to push
     * @param message to push
     */
    private void pushToUser(String channel, List<String> users, Json message) {

        if (channel == null || message == null) {
            return;
        }

        for (Entry<String, SessionData> entry : SocketSessions.getSessions().entrySet()) {
            try {
                SessionData datas = entry.getValue();
                Session session = datas.getSession();
                String user_id = (String) session.getUserProperties().get("user_id");
                if (users.contains(user_id)) {
                    if (datas.getChannels().contains(channel)) {
                        SocketMessage data = new SocketMessage(channel);
                        if (message.get("notification") != null) {
                            Json notification = message.getJson("notification");
                            notification.put("title", Language.get(notification.getString("title"), session.getUserProperties().get("hl").toString()));
                            notification.put("message", Language.get(notification.getString("message"), session.getUserProperties().get("hl").toString()));
                            Json message_clone = message.clone();
                            message_clone.put("notification", notification);
                            data.setMessage(message_clone);
                        } else {
                            data.setMessage(message);
                        }
                        session.getAsyncRemote().sendText(data.toString());
                    }
                    if (message.getString("action", "").equals("logout")) {
                        datas.getSession().close(new CloseReason(() -> 401, "Session ended by other"));
                    }
                }

            } catch (Exception e) {
                if (Fx.IS_DEBUG) {
                    e.printStackTrace();
                }
            }
        }


    }

}
