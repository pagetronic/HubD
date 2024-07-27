/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.profile;

import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;

import java.util.Date;

public class CookiesUtils {

    /**
     * log consent (RGPD/GDPR usage)
     */
    public static Json consent(String id, String ip, boolean accept, String choice) {
        return new Json("ok", Db.save("Consents", new Json("uid", id).put("ip", ip).put("date", new Date()).put("consent", accept).put("type", choice)));
    }

}
