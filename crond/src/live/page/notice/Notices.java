/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.notice;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import live.page.web.system.Settings;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bson.conversions.Bson;

import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Notices {

	private static final ExecutorService service = Executors.newSingleThreadExecutor();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			Fx.shutdownService(service);
		}));
	}

	/**
	 * Notification croner
	 * TODO: bugs
	 */
	public static void cron() {
		service.submit(() -> {

			while (true) {

				Aggregator grouper = new Aggregator("ids", "user", "title", "message", "config", "tag", "date", "delay");
				List<Bson> pipeline = new ArrayList<>();
				pipeline.add(Aggregates.match(
						Filters.and(
								Filters.exists("read", false),
								Filters.exists("received", false),
								Filters.ne("config", null),
								Filters.ne("delay", null),
								Filters.lt("delay", new Date())
						)
				));
				pipeline.add(Aggregates.group(
						new Json("tag", "$tag").put("user", "$user").put("config", "$config.auth")
						, grouper.getGrouper(
								Accumulators.first("id", "$_id"),
								Accumulators.push("ids", "$_id"),
								Accumulators.push("title", "$title"),
								Accumulators.push("message", "$message")
						)));

				pipeline.add(Aggregates.project(grouper.getProjection().put("_id", "$id")));
				pipeline.add(Aggregates.sort(Sorts.descending("date")));
				MongoCursor<Json> notices = Db.aggregate("Notices", pipeline).iterator();
				while (notices.hasNext()) {
					Json notice = notices.next();
					try {
						webPush(notice);
					} catch (Exception e) {
						Fx.log("WebPush error " + e.getMessage());
					}
					Db.updateMany("Notices", Filters.in("_id", notice.getList("ids")), new Json("$unset", new Json("delay", "")));

				}
				notices.close();

				Thread.sleep(3000);

			}

		});
	}

	/**
	 * Send webpush notification to suscribers
	 */
	private static void webPush(Json data) throws Exception {


		String tag = data.getString("tag");
		List<String> titles = data.getList("title");
		List<String> messages = data.getList("message");
		String title = Fx.truncate(StringUtils.join(titles, " / "), 2000);
		String body = Fx.truncate(StringUtils.join(messages, " / "), 2000);

		Json config = data.getJson("config");

		Security.addProvider(new BouncyCastleProvider());

		Json notice = new Json();
		notice.put("title", StringEscapeUtils.unescapeXml(title));
		notice.put("body", StringEscapeUtils.unescapeXml(body));

		notice.put("url", (data.getString("lng") == null ? "" : Settings.getFullHttp(data.getString("lng"))) + "/notices/" + data.getId());
		notice.put("icon", Settings.getLogo() + "@256x256");

		if (tag != null) {
			notice.put("tag", tag);
		} else {
			notice.put("tag", Fx.getUnique());
		}

		Notification notification = new Notification(config.getText("endpoint").replace("\n", ""), config.getText("key"), config.getText("auth"), notice.toString(true));
		PushService pushService = new PushService();

		pushService.setPublicKey(Settings.VAPID_PUB);
		pushService.setPrivateKey(Settings.VAPID_PRIV);
		pushService.setGcmApiKey(Settings.GOOGLE_PUSH_KEY);
		String endpoint = config.getText("endpoint", "");

		if (endpoint.contains("google")) {
			endpoint = endpoint.replace("gcm-http.googleapis.com/gcm/", "fcm.googleapis.com/fcm/")
					.replace("gcm-xmpp.googleapis.com/gcm/", "fcm.googleapis.com/fcm/")
					.replace("android.clients.google.com/gcm/send", "fcm.googleapis.com/fcm/send")
					.replaceAll("android.apis.google.com/([^/]+)/send", "fcm.googleapis.com/fcm/send")
					.replace("android.googleapis.com/gcm/send", "fcm.googleapis.com/fcm/send");
			config.put("endpoint", endpoint);
		}

		pushService.sendAsync(notification);


	}
}
