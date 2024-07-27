/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.admin;

import live.page.hubd.system.Language;
import live.page.hubd.system.Settings;
import live.page.hubd.system.cosmetic.UiStyleServlet;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.utils.Fx;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LangsAdmin {

    private static Json langs = null;

    static {
        try {
            langs = new Json(FileUtils.readFileToString(new File((Settings.REPO) + "/res/langs.json"), StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void doGetAuth(WebServletRequest req, WebServletResponse resp) throws IOException {
        if (!Fx.IS_DEBUG) {
            resp.sendError(404);
            return;
        }

        req.setAttribute("admin_active", "langs");

        req.setAttribute("langs", langs);

        req.setAttribute("escape", StringEscapeUtils.class);

        req.setTitle(Fx.ucfirst(Language.get("LANGUAGES", req.getLng())));
        resp.sendTemplate(req, "/admin/langs.html");

    }

    public static void doPostApiAdmin(ApiServletResponse resp, Json data) throws IOException {

        new Json("error", "INVALID_DATA");
        Json rez = switch (data.getString("action")) {
            case "create" -> create(data);
            case "remove" -> remove(data);
            case "update" -> update(data);
            default -> new Json("error", "INVALID_DATA");
        };

        UiStyleServlet.buildJs();
        resp.sendResponse(rez);

    }

    private static Json create(Json data) {

        String key = "0";
        while (langs.containsKey(key)) {
            key += "0";
        }
        langs.prepend(key, new Json());
        saveLangs();

        return new Json("ok", true);
    }

    private static Json update(Json data) {
        Json rez = new Json();

        if (data.containsKey("js")) {
            if (data.getBoolean("js", false)) {
                langs.set(data.getString("key"), langs.getJson(data.getString("key")).put("js", true));
            } else {
                langs.set(data.getString("key"), langs.getJson(data.getString("key")).remove("js"));
            }
            rez.put("js", data.getBoolean("js", false));
        } else if (data.getString("lang", "").equals("key")) {
            String new_key = data.getString("value").toUpperCase().replace(" ", "_");
            langs.set(new_key, langs.get(data.getString("key")));
            langs.remove(data.getString("key"));
            langs.sort();
            rez.put("value", new_key);
        } else if (data.containsKey("context")) {
            langs.set(data.getString("key"), langs.getJson(data.getString("key")).put("context", data.getText("context", "")));
            rez.put("value", data.getText("context", ""));
        } else {
            langs.set(data.getString("key"), langs.getJson(data.getString("key")).put(data.getString("lang"), data.getString("value")));
            rez.put("value", StringEscapeUtils.escapeXml11(data.getText("value")));
        }
        saveLangs();

        return rez;
    }

    private static Json remove(Json data) {
        langs.remove(data.getString("key"));
        saveLangs();
        return new Json("ok", true);
    }


    private static void saveLangs() {
        try {
            Json langs_ = langs.clone().sort();
            for (String key : langs_.keyList()) {
                Json lang = langs_.getJson(key).sort();
                lang.prepend("en", lang.getString("en"));
                lang.prepend("fr", lang.getString("fr"));
                lang.prepend("context", lang.getString("context"));
                if (lang.getBoolean("js", false)) {
                    lang.put("js", true);
                } else {
                    lang.remove("js", true);
                }
                langs_.set(key, lang);
            }
            FileUtils.writeStringToFile(new File(Settings.REPO + "/res/langs.json"), langs_.toString(false), StandardCharsets.UTF_8);
            Language.rebuild();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
