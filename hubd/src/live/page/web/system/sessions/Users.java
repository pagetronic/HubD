/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.sessions;

import live.page.web.system.json.Json;

import java.util.List;

public class Users extends Json {

	public Users() {
	}

	public Users(Json data) {
		super(data);
	}

	public boolean getAdmin() {
		if (getListJson("teams") == null) {
			return false;
		}
		for (Json team : getListJson("teams")) {
			if (team.getBoolean("admin", false)) {
				return true;
			}
		}
		return false;
	}

	public boolean getEditor() {
		if (getAdmin()) {
			return true;
		}
		if (getListJson("teams") == null) {
			return false;
		}
		for (Json team : getListJson("teams")) {
			if (team.getBoolean("editor", false)) {
				return true;
			}
		}
		return false;
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
}
