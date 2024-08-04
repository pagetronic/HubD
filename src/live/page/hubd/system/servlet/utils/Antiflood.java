/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet.utils;

import live.page.hubd.system.Settings;
import live.page.hubd.system.utils.Fx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Antiflood {

    private final static Map<String, Long> antiflood = new ConcurrentHashMap<>();

    public static boolean isFlood(String user_id) {
        if (Fx.IS_DEBUG) {
            return false;
        }
        try {
            antiflood.forEach((user, date) -> {
                if (date < System.currentTimeMillis() - Settings.FLOOD_DELAY) {
                    antiflood.remove(user);
                }
            });
            return antiflood.containsKey(user_id);
        } catch (Exception e) {
            e.printStackTrace();
            antiflood.clear();
            return false;
        } finally {
            antiflood.put(user_id, System.currentTimeMillis());
        }
    }
}

