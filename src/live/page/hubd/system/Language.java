/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system;

import live.page.hubd.system.json.Json;
import live.page.hubd.system.utils.Fx;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Language {

    private static final Map<String, Json> langs = build();

    /**
     * Test if Language translation exist
     *
     * @param key of the translation
     * @param lng language wanted
     * @return true|false if exist
     */
    public static boolean exist(String key, String lng) {
        return langs.containsKey(lng) && langs.get(lng).containsKey(key);
    }

    /**
     * Get language string for a specific key
     *
     * @param key        of the translation
     * @param lng        language wanted
     * @param parameters array of strings to replace in order : %1,%2,%3...
     * @return translation
     */
    public static String get(String key, String lng, Object... parameters) {
        List<String> lngs = new ArrayList<>(Settings.getLangs());
        lngs.remove(lng);
        lngs.add(0, lng);
        for (String lang : lngs) {
            if (key != null && !key.isEmpty() && langs.containsKey(lang) && langs.get(lang).containsKey(key)) {
                Json tags = langs.get(lang);
                String str = tags.getString(key, "");
                for (int i = 0; i < parameters.length; i++) {
                    str = str.replace("%" + (i + 1), String.valueOf(parameters[i]));
                }
                if (Fx.IS_DEBUG && !lang.equals(lng)) {
                    Fx.log("Langue " + key + " does not exists");
                }
                return str;
            }
        }
        if (Fx.IS_DEBUG) {
            Fx.log("Langue " + key + " does not exists");
        }

        return "${" + key + "." + lng + "}";
    }


    /**
     * Build language data from file
     */
    private static Map<String, Json> build() {
        Map<String, Json> langs = new HashMap<>();
        for (String lng : Settings.getLangs()) {
            Json lang = new Json();
            try {
                Iterator<URL> it = Thread.currentThread().getContextClassLoader().getResources("/res/lng/" + lng + ".json").asIterator();
                while (it.hasNext()) {
                    Json before = lang.clone();
                    lang.putAll(new Json(IOUtils.toString(it.next(), StandardCharsets.UTF_8)));
                    lang.putAll(before);
                }
            } catch (IOException ignored) {
            }
            langs.put(lng, lang);
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
