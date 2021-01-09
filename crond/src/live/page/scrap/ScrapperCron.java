package live.page.scrap;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import live.page.web.admin.utils.Scrapper;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScrapperCron {

	private final static ExecutorService services = Executors.newSingleThreadExecutor();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> Fx.shutdownService(services)));
	}

	public static void scrap() {
		services.submit(() -> {
			MongoCursor<Json> scraps = Db.find("Scraps").sort(Sorts.ascending("last")).iterator();
			while (scraps.hasNext()) {
				if (services.isTerminated() || services.isShutdown()) {
					break;
				}
				Json scrap = scraps.next();
				Fx.log("Scrap " + scrap.getString("url"));
				Scrapper.scrapAndPost(scrap);
			}
			scraps.close();
			Fx.log("Scrap sleep " + new Date().toString());

			try {
				Thread.sleep(10L * 60L * 1000L);

			} catch (InterruptedException ignore) {
			}

			if (!services.isTerminated() && !services.isShutdown()) {
				scrap();
			}
		});
	}
}
