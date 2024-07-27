/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system;

import live.page.hubd.system.json.Json;
import live.page.hubd.utils.Fx;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Language {

    private static final Json langs = build();

    /**
     * Test if Language translation exist
     *
     * @param key of the translation
     * @param lng language wanted
     * @return true|false if exist
     */
    public static boolean exist(String key, String lng) {
        return langs.containsKey(key) && langs.getJson(key).containsKey(lng);
    }

    /**
     * Get language string for a specific key
     *
     * @param key      of the translation
     * @param lng_     language wanted
     * @param replaces array of strings to replace in order : %1,%2,%3...
     * @return
     */
    public static String get(String key, String lng_, Object... replaces) {

        if (lng_ == null) {
            return "${" + key + ".null}";
        }

        String[] lngs = lng_.split("_");

        String country;
        String lng;

        if (lngs.length > 1) {
            lng = lngs[0];
            country = lngs[1];
        } else {
            lng = lng_;
            country = null;
        }
        if (key != null && !key.isEmpty() && lng != null && langs.get(key) != null) {
            Json tag = langs.getJson(key);
            String str = country != null ? tag.getString(country, tag.getString(lng, null)) : tag.getString(lng, null);
            for (int i = 0; i < replaces.length; i++) {
                str = str.replace("%" + (i + 1), String.valueOf(replaces[i]));
            }
            return str;
        } else {
            if (Fx.IS_DEBUG) {
                Fx.log("Langue " + key + " does not exists");
            }
            return "${" + key + "." + lng_ + "}";
        }
    }

    /**
     * Get language for JavaScript
     *
     * @return Json string
     */
    public static String getLangsJs() {
        Json langs_js = new Json();
        langs.keySet().forEach(key -> {
            Json lang = langs.getJson(key);
            if (lang.getBoolean("js", false)) {
                langs_js.put(key, lang.remove("js"));
            }
        });
        langs_js.put("lng", Settings.getLangs());
        return langs_js.toString(true) + ";";
    }

    /**
     * Build language data from file
     */
    private static Json build() {
        Json langs = new Json();

        try {
            try {
                langs.putAll(new Json(FileUtils.readFileToString(new File(Settings.REPO + "/res/langs.json"), StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception ignore) {
        }

        return langs;
    }

    /**
     * Rebuild language data from file
     */
    public static void rebuild() {
        langs.clear();
        langs.putAll(build());
    }
}
