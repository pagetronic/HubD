/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.servlet.utils;

import com.mongodb.client.model.Filters;
import live.page.web.db.Db;
import live.page.web.socket.SocketMessage;
import live.page.web.utils.Fx;
import live.page.web.utils.json.Json;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@WebListener
public class LogsUtils implements ServletContextListener {


	private static final ExecutorService asyncService = Executors.newFixedThreadPool(20);

	public static void pushLog(ServletRequest request) {

		HttpServletRequest req = (HttpServletRequest) request;
		Json log = new Json();
		log.put("u", req.getScheme() + "://" + req.getServerName() + req.getRequestURI());
		log.put("p", req.getParameterMap());
		log.put("m", req.getMethod());
		log.put("i", ServletUtils.realIp(req));
		log.put("d", new Date());
		if (req.getHeaderNames() != null) {
			Enumeration<String> headers_names = req.getHeaderNames();
			while (headers_names.hasMoreElements()) {
				String name = headers_names.nextElement();
				log.add("h", new Json(name, req.getHeader(name)));
			}
		}
		asyncService.submit(() -> Db.getDb("Logs").insertOne(log));
	}

	public static SocketMessage pushStats(String act, String ip, Json data) {
		if (data.containsKey("gone")) {
			Db.updateOne("Stats", Filters.eq("_id", data.getId()), new Json("$set", new Json(data.getBoolean("gone", false) ? "gone" : "alive", new Date())));
			return new SocketMessage();
		}
		Json stat = new Json();
		stat.put("sysid", data.getString("sysid"));
		stat.put("url", data.getString("location"));
		stat.put("width", data.getInteger("width"));
		stat.put("height", data.getInteger("height"));
		stat.put("ua", data.getString("ua"));
		stat.put("ip", ip);
		if (data.getString("user") != null) {
			stat.put("user", data.getString("user"));
		}
		stat.put("date", new Date());
		stat.put("_id", Db.getKey());
		asyncService.submit(() -> Db.getDb("Stats").insertOne(stat));
		return new SocketMessage(act).addMessage("_id", stat.getId());
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Fx.shutdownService(asyncService);
	}
}