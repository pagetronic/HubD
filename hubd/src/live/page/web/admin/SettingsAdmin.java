/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import live.page.web.system.Language;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;
import org.apache.commons.io.FileUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@WebServlet(urlPatterns = {"/admin/settings"})
public class SettingsAdmin extends HttpServlet {

	private final static Json settings = new Json();


	@Override
	public void doGetEditor(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

		req.setAttribute("admin_active", "settings");
		req.setTitle(Fx.ucfirst(Language.get("SETTINGS", req.getLng())));

		req.setAttribute("settings", getSettings());

		resp.sendTemplate(req, "/admin/settings.html");
	}

	@Override
	public void doPostApiEditor(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {

		Json rez = new Json("error", "INVALID_DATA");
		switch (data.getString("action", "")) {
			case "save":
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
				break;
		}

		reload();

		resp.sendResponse(rez);


	}

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
			} else if (!FileUtils.readFileToString(file).equals(settings.toString())) {
				FileUtils.writeStringToFile(file, new Json("settings", settings).toString());
				Fx.log("Settings backed in " + file.getAbsolutePath() + " at " + Fx.UTCDate());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
