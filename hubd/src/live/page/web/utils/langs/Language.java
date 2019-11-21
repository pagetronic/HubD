/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.utils.langs;

import live.page.web.utils.Fx;
import live.page.web.utils.Settings;
import live.page.web.utils.json.Json;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class Language {

	private static final Json langs = built();


	public static boolean exist(String key, String lng) {
		return langs != null && langs.containsKey(key) && langs.getJson(key).containsKey(lng);
	}

	public static String get(String key, String lng, Object... replaces) {
		if (lng == null) {
			return "${" + key + ".null}";
		}
		if (key != null && !key.equals("") && lng != null && langs.get(key) != null) {
			String str = langs.getJson(key).getString(lng, "");
			for (int i = 0; i < replaces.length; i++) {
				str = str.replace("%" + (i + 1), String.valueOf(replaces[i]));
			}
			return str;
		} else {
			if (Fx.IS_DEBUG) {
				Fx.log("Langue " + key + " does not exists");
			}
			return "${" + key + "." + lng + "}";
		}
	}

	public static String getLangsJs() {
		Json langs_js = new Json();
		langs.keySet().forEach(key -> {
			Json lang = langs.getJson(key);
			if (lang.getBoolean("js", false)) {
				langs_js.put(key, lang.remove("js"));
			}
		});
		return langs_js.toString(true) + ";";
	}

	private static Json built() {
		Json langs = new Json();

		try {
			langs.putAll(new Json(FileUtils.readFileToString(new File(Settings.HUB_REPO + "/res/langs.json"))));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			langs.putAll(new Json(FileUtils.readFileToString(new File(Settings.REPO + "/res/langs.json"))));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return langs;
	}

	public static Json getLangs() {
		return langs;
	}

	public static void rebuilt() {
		langs.clear();
		langs.putAll(built());
	}
}
