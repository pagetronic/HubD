/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.socket;

import com.mongodb.CursorType;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Sorts;
import live.page.web.system.Language;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;
import org.bson.Document;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.websocket.Session;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * For multi-server system, you can't have a live websocket update if you don't store update in a common system to all servers
 *
 * Here we use Capped Collection from MongodDB https://docs.mongodb.com/manual/core/capped-collections/
 * and Tailable Cursor https://docs.mongodb.com/manual/core/tailable-cursors/
 *
 * Every push are live.
 * In Javascript use : socket.follow('channel/tofollow', function (msg) {});
 */
@WebListener
public class SocketCapper implements ServletContextListener {

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
							if (push.containsKey("channel")) {
								pushToUser(push.getString("channel"), push.getList("users"), push.getJson("message"));
							} else {
								pushToAll(push.getJson("message"), push.getList("excludes"));
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
	private void pushToAll(Json message, List<String> excludes) {
		for (Entry<String, SessionData> entry : SocketSessions.getSessions().entrySet()) {
			SessionData data = entry.getValue();
			if (!excludes.contains(data.getUserId())) {
				Session session = data.getSession();
				session.getAsyncRemote().sendText(message.toString());
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
		if ((channel == null) || (message == null)) {
			return;
		}
		for (Entry<String, SessionData> entry : SocketSessions.getSessions().entrySet()) {
			try {
				SessionData datas = entry.getValue();
				if (datas.getElements().contains(channel)) {
					Session session = datas.getSession();
					String user_id = (String) session.getUserProperties().get("user_id");
					if (users == null || users.contains(user_id)) {
						SocketMessage data = new SocketMessage(channel);
						if (message.get("notification") != null) {
							Json notification = message.getJson("notification");
							notification.put("title", Language.get(notification.getString("title"), session.getUserProperties().get("hl").toString()));
							notification.put("message", Language.get(notification.getString("message"), session.getUserProperties().get("hl").toString()));
							Json message_clone = message.clone();
							message_clone.put("notification", notification);
							data.put("message", message_clone);
						} else {
							data.put("message", message);
						}
						session.getAsyncRemote().sendText(data.toString());

					}
				}
			} catch (Exception ignored) {
			}
		}
	}

}
