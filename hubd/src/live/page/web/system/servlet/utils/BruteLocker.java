/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.servlet.utils;

import live.page.web.utils.Fx;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Lock access for a time
 */
@WebListener
public class BruteLocker implements ServletContextListener {

	public static final int DELAY = 6; // lock for 6 seconds
	private static final List<String> banned = new ArrayList<>();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		scheduler.scheduleAtFixedRate(banned::clear, DELAY, DELAY, TimeUnit.SECONDS);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Fx.shutdownService(scheduler);
	}

	public static void add(String remoteAddr) {
		banned.add(remoteAddr);
		Fx.log("banned:" + remoteAddr);
	}

	public static boolean isBan(String remoteAddr) {
		return banned.contains(remoteAddr);
	}
}
