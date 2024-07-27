/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.sessions;

import com.mongodb.client.model.Filters;
import live.page.hubd.system.json.Json;
import org.bson.conversions.Bson;

import java.util.List;

public class Users extends Json {

    public Users() {
    }

    public Users(Json data) {
        super(data);
        if (getListJson("groups") != null) {
            for (Json team : getListJson("groups")) {
                if (team.getBoolean("admin", false)) {
                    put("admin", true);
                    break;
                }
            }
        }
    }

    public boolean getAdmin() {

        if (getListJson("groups") == null) {
            for (Json team : getListJson("groups")) {
                if (team.getBoolean("admin", false)) {
                    return true;
                }
            }
        }

        if (getListJson("original") != null) {
            for (Json original : getListJson("original")) {
                if (getListJson("groups") != null) {
                    for (Json team : original.getListJson("groups")) {
                        if (team.getBoolean("admin", false)) {
                            return true;
                        }
                    }
                }
            }
        }

        return getBoolean("admin", false);
    }

    public String getSetting(String key, String def) {
        if (getJson("settings") == null || getJson("settings").getString(key) == null) {
            return def;
        }
        return getJson("settings").getString(key);
    }


    public boolean scope(String scope) {
        List<String> scopes = getList("scopes");
        if (scope == null || scopes == null) {
            return false;
        }
        return scopes.contains(scope);
    }

    public Bson getViewFilter() {
        return Filters.or(
                Filters.or(
                        Filters.eq("shares", getId()),
                        Filters.eq("shares", null)
                ),
                Filters.eq("user", getId()),
                Filters.eq("users", getId())
        );
    }

    public Bson getEditFilter() {
        return Filters.or(
                Filters.eq("user", getId()),
                Filters.eq("users", getId())
        );
    }

}
