/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin;

import live.page.web.servlet.wrapper.ApiServletResponse;
import live.page.web.servlet.wrapper.WebServletRequest;
import live.page.web.servlet.wrapper.WebServletResponse;
import live.page.web.utils.Fx;
import live.page.web.utils.Settings;
import live.page.web.utils.google.Translater;
import live.page.web.utils.json.Json;
import live.page.web.utils.langs.Language;
import live.page.web.servlet.style.UiStyleServlet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;

public class LangsAdmin {

	public static void doGetEditor(WebServletRequest req, WebServletResponse resp) throws IOException {
		if (!Fx.IS_DEBUG) {
			resp.sendError(404);
			return;
		}

		req.setAttribute("admin_active", "langs");
		req.setAttribute("langs_availables", Settings.getLangs());

		req.setAttribute("langs", getLangs(req.getString("local", null) != null));

		req.setAttribute("escape", StringEscapeUtils.class);

		req.setTitle(Fx.ucfirst(Language.get("LANGUAGES", req.getLng())));
		resp.sendTemplate(req, "/admin/langs.html");

	}

	public static void doPostApiAdmin(ApiServletResponse resp, Json data) throws IOException {

		Json rez = new Json("error", "INVALID_DATA");
		switch (data.getString("action")) {
			case "create":
				rez = create(data);
				break;
			case "translate":
				rez = translate(data);
				break;
			case "remove":
				rez = remove(data);
				break;
			case "update":
				rez = update(data);
				break;
		}

		UiStyleServlet.buildJs();
		resp.sendResponse(rez);

	}

	private static Json translate(Json data) {
		boolean local = data.getBoolean("local", false);
		Json langs = getLangs(local);
		Json values = langs.getJson(data.getString("key"));
		String srclng = !data.getString("lng", "").equals("en") && !values.getString("en", "").equals("") ? "en" : "fr";
		String translation = Translater.translate(values.getString(srclng), srclng, data.getString("lng"));
		if (translation == null) {
			return new Json("ok", false).put("translation", values.getString(data.getString("lng")));
		}
		translation = translation.replace("% ", " %");
		langs = getLangs(local);
		values = langs.getJson(data.getString("key"));
		values.put(data.getString("lng"), translation);
		langs.put(data.getString("key"), values);
		setLangs(langs, local);
		return new Json("ok", true).put("translation", translation);

	}

	private static Json create(Json data) {

		boolean local = data.getBoolean("local", false);
		Json langs = getLangs(local);
		String key = " ";
		while (langs.containsKey(key)) {
			key += " ";
		}
		langs.put(key, new Json());
		setLangs(langs, local);

		return new Json("ok", true);
	}

	private static Json update(Json data) {
		boolean local = data.getBoolean("local", false);
		Json rez = new Json();
		Json langs = getLangs(local);
		Json lang = langs.getJson(data.getString("key"));

		if (data.containsKey("js")) {
			if (data.getBoolean("js", false)) {
				lang.put("js", true);
			} else {
				lang.remove("js");
			}
			langs.put(data.getString("key"), lang);
			rez.put("js", data.getBoolean("js", false));

		} else if (data.getString("lng").equals("key")) {

			String key = data.getString("value").toUpperCase().replace(" ", "_");
			langs.remove(data.getString("key"));
			langs.put(key, lang);
			rez.put("value", key);

		} else {

			lang.put(data.getString("lng"), data.getString("value"));
			langs.put(data.getString("key"), lang);
			rez.put("value", StringEscapeUtils.escapeXml11(data.getText("value")));
		}
		setLangs(langs.sort(), local);

		return rez;
	}

	private static Json remove(Json data) {
		boolean local = data.getBoolean("local", false);
		Json langs = getLangs(local);
		langs.remove(data.getString("key"));
		setLangs(langs, local);
		return new Json("ok", true);
	}

	private static Json getLangs(boolean local) {
		try {
			return new Json(FileUtils.readFileToString(new File((local ? Settings.REPO : Settings.HUB_REPO) + "/res/langs.json")));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void setLangs(Json langs, boolean local) {
		try {
			FileUtils.writeStringToFile(new File((local ? Settings.REPO : Settings.HUB_REPO) + "/res/langs.json"), langs.sort().toString(false));
			Language.rebuilt();
		} catch (Exception e) {
		}
	}

}
