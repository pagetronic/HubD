/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.admin;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.Language;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.utils.Fx;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@WebServlet(asyncSupported = true, urlPatterns = {"/admin/settings"})
public class SettingsAdmin extends HttpServlet {

    private final static Json settings = new Json();

    public static double getMarginIndice() {
        return (100D - getSettings().getDouble("MARGIN", 0)) / 100D;
    }

    public static String getString(String key) {
        return getSettings().getText(key);
    }

    public static boolean getBoolean(String key) {
        return getSettings().getBoolean(key, false);
    }

    public static int getInteger(String key) {
        return getSettings().getInteger(key);
    }

    public static double getDouble(String key) {
        return getSettings().getDouble(key);
    }

    public static Date getDelay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MILLISECOND, SettingsAdmin.getInteger("DELIVERY_TIME"));
        return cal.getTime();
    }

    public static void set(String key, Object value) {
        Json set = Db.findById("Settings", key.toUpperCase());
        if (set == null) {
            set = new Json();
            set.put("_id", key.toUpperCase());
            set.put("explain", null);
            set.put("value", value);
            Db.getDb("Settings").insertOne(set);
        } else {
            set.put("value", value);
            Db.save("Settings", set);
        }
        settings.put(key, value);
        backup();
    }

    public static Json getSettings() {
        if (settings.isEmpty()) {
            MongoCursor<Json> settings_it = Db.find("Settings").iterator();
            while (settings_it.hasNext()) {
                Json setting = settings_it.next();
                settings.put(setting.getId(), setting.get("value"));

            }
            settings_it.close();
        }
        return settings;
    }

    public static Json getJson(String key) {
        return getSettings().getJson(key.toUpperCase());
    }

    public static List<Json> getListJson(String key) {
        return getSettings().getListJson(key.toUpperCase());
    }

    private static void reload() {
        settings.clear();
        getSettings();
        backup();
    }

    private static void backup() {
        if (!Fx.IS_DEBUG) {
            return;
        }
        try {
            List<Json> settings = Db.find("Settings").into(new ArrayList<>());
            File file = new File(Settings.REPO + "/res/settings.json");
            if (!file.exists()) {
                Fx.log("No file : " + file.getAbsolutePath());
            } else if (!FileUtils.readFileToString(file, StandardCharsets.UTF_8).equals(settings.toString())) {
                FileUtils.writeStringToFile(file, new Json("settings", settings).toString(), StandardCharsets.UTF_8);
                Fx.log("Settings backed in " + file.getAbsolutePath() + " at " + Fx.UTCDate());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {
        if (!user.getAdmin()) {
            resp.sendError(401, "NO_AUTHORIZATION");
            return;
        }
        req.setAttribute("admin_active", "settings");
        req.setTitle(Fx.ucfirst(Language.get("SETTINGS", req.getLng())));

        req.setAttribute("settings", getSettings());

        resp.sendTemplate(req, "/admin/settings.html");
    }

    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {
        if (!user.getAdmin()) {
            resp.sendError(401, "NO_AUTHORIZATION");
            return;
        }
        Json rez = new Json("error", "INVALID_DATA");
        if (data.getString("action", "").equals("save")) {
            String value_ = data.getString("value");
            Object value = value_;
            if (data.getString("type", "").equals("integer")) {
                value = Integer.valueOf(value_);
            }
            if (data.getString("type", "").equals("double")) {
                value = Double.valueOf(value_);
            }
            if (data.getString("type", "").equals("boolean")) {
                value = Boolean.valueOf(value_);
            }
            rez = new Json("ok", Db.updateOne("Settings", Filters.eq("_id", data.getString("key")), new Json("$set", new Json("value", value))).getMatchedCount() > 0).put("value", value);
            reload();
        }

        reload();

        resp.sendResponse(rez);


    }

    @Override
    public void init(ServletConfig config) {
        try {
            List<Json> settings = new Json(Fx.getResource("/res/settings.json")).getListJson("settings");
            List<String> ids = new ArrayList<>();
            for (Json setting : settings) {
                ids.add(setting.getId());
                setting.put("_id", setting.getId()).remove("id");
                if (!Db.exists("Settings", Filters.eq("_id", setting.getId()))) {
                    Db.getDb("Settings").insertOne(setting);
                } else {
                    Db.getDb("Settings").replaceOne(Filters.eq("_id", setting.getId()), setting);
                }
            }
            Db.deleteMany("Settings", Filters.nin("_id", ids));
            reload();
        } catch (Exception e) {

        }
    }

    @Override
    public void destroy() {
        settings.clear();
    }


}
