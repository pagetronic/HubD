/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.servlet.utils;

import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;

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

	@Override
	public void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Fx.shutdownService(asyncService);
	}
}