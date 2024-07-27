/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet.utils;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpServletRequest;
import live.page.hubd.utils.Fx;

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

    public static void add(String remoteAddr) {
        banned.add(remoteAddr);
        Fx.log("banned:" + remoteAddr);
    }

    public static void add(HttpServletRequest req) {
        add(ServletUtils.realIp(req));
    }

    public static boolean isBan(String remoteAddr) {

        return banned.contains(remoteAddr);

    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler.scheduleAtFixedRate(banned::clear, DELAY, DELAY, TimeUnit.SECONDS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Fx.shutdownService(scheduler);
    }
}
