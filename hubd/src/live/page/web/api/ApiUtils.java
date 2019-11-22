/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.api;

import com.mongodb.client.model.*;
import live.page.web.db.Db;
import live.page.web.session.Users;
import live.page.web.utils.Fx;
import live.page.web.utils.json.Json;
import org.bson.conversions.Bson;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ApiUtils {
	public static Json removeAccess(String acc_id, Users user) {
		Json accs = Db.find("ApiAccess", Filters.and(Filters.eq("_id", acc_id), Filters.eq("user", user.getId()))).first();
		if (accs != null) {
			Db.deleteOne("ApiAccess", accs);
			return new Json("ok", true);
		}

		return new Json("ok", false);
	}

	public static Json refreshAccess(String acc_id, Users user) {
		Json accs = Db.findOneAndUpdate("ApiAccess", Filters.and(Filters.eq("_id", acc_id), Filters.eq("user", user.getId())),
				new Json("$set", new Json("access_token", Fx.getSecureKey()).put("refresh_token", Fx.getSecureKey()).put("expire", new Date(System.currentTimeMillis() + 3600000))),
				new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
		if (accs != null) {
			return new Json("ok", true);
		}

		return new Json("ok", false);
	}

	public static Json createApps(Users user, String name, String redirect_uri, String scope) {

		List<String> scopes = parseScope(scope);
		List<Bson> filters = new ArrayList<>();
		filters.add(Filters.eq("user", user.getId()));

		filters.add(Filters.exists("client_id", true));
		filters.add(Filters.exists("client_secret", true));

		if (redirect_uri != null) {
			filters.add(Filters.eq("redirect_uri", redirect_uri));
		}
		if (scopes != null) {
			filters.add(Filters.eq("scopes", scopes));
		}
		Json app = Db.find("ApiApps", Filters.and(filters)).first();

		if (app == null) {
			app = new Json();
			app.put("name", name == null ? user.getString("name") + " App" : Fx.truncate(name, 255));
			String client_id = Fx.getSecureKey();
			while (Db.exists("ApiApps", Filters.eq("client_id", client_id))) {
				client_id = Fx.getSecureKey();
			}
			app.put("client_id", client_id);
			app.put("client_secret", Fx.getSecureKey());
			app.put("date", new Date());
			app.put("user", user.getId());
			app.put("scopes", scopes == null ? Scopes.scopes : scopes);
			app.add("redirect_uri", redirect_uri);
			Db.save("ApiApps", app);
		}

		return new Json("ok", true).put("client_id", app.getString("client_id")).put("client_secret", app.getString("client_secret"));
	}

	public static Json getAccess(String app_id, Users user) {

		Json app = null;
		if (app_id != null) {
			app = Db.find("ApiApps", Filters.and(Filters.eq("_id", app_id), Filters.eq("user", user.getId()))).first();
		}
		if (app == null) {
			return new Json("error", "NO_APPS");

		}


		Json access = new Json();
		Date date = new Date();
		Date expire = new Date(date.getTime() + 3600 * 1000);
		access.put("access_token", Fx.getSecureKey());
		access.put("refresh_token", Fx.getSecureKey());
		access.put("expire", expire);
		access.put("date", date);
		access.put("user", user.getId());
		access.put("client_id", app.getString("client_id"));
		access.put("client_secret", app.getString("client_secret"));
		access.put("force", true);
		access.put("scopes", app.getList("scopes"));
		Db.save("ApiAccess", access);

		return new Json("ok", true).put("id", access.getId());
	}

	public static Json renameApps(String app_id, String name, Users user) {
		Json app = null;
		if (app_id != null) {
			app = Db.find("ApiApps", Filters.and(Filters.eq("_id", app_id), Filters.eq("user", user.getId()))).first();
		}
		if (app == null) {
			return new Json("error", "no apps");

		}
		if (name != null) {
			name = Jsoup.clean(name, new Whitelist());
			if (!name.equals("")) {
				app.put("name", name);
				Db.save("ApiApps", app);
				return new Json("ok", true).put("name", app.getString("name"));
			}
		}
		return new Json("ok", false);
	}

	public static Json deleteApps(String app_id, Users user) {
		Json app = null;
		if (app_id != null) {
			app = Db.find("ApiApps", Filters.and(Filters.eq("_id", app_id), Filters.eq("user", user.getId()))).first();
		}
		if (app == null) {
			return new Json("error", "no apps");

		}
		app.put("client_id_before", app.getString("client_id"));
		app.put("client_secret_before", app.getString("client_secret"));
		app.put("removed", true);
		Db.deleteMany("ApiAccess", Filters.eq("client_id", app.getString("client_id")));
		app.remove("client_id");
		app.remove("client_secret");
		Db.save("ApiApps", app);
		return new Json("ok", true);
	}

	public static Json changeSecret(String app_id, Users user) {
		Json app = null;
		if (app_id != null) {
			app = Db.find("ApiApps", Filters.and(Filters.eq("_id", app_id), Filters.eq("user", user.getId()))).first();
		}
		if (app == null) {
			return new Json("error", "no apps");

		}
		app.put("client_secret_before", app.getString("client_secret"));
		app.put("client_secret", Fx.getSecureKey().substring(0, 12) + Fx.getSecureKey());
		Db.save("ApiApps", app);
		return new Json("ok", true).put("client_secret", app.get("client_secret"));
	}

	public static Json redirectUri(String appid, String type, String redirect_uri, Users user) {

		if (type != null && redirect_uri != null) {
			Json accs = Db.findOneAndUpdate("ApiApps", Filters.and(Filters.eq("_id", appid), Filters.eq("user", user.getId())),
					new Json(type.equals("add") ? "$push" : "$pull", new Json("redirect_uri", redirect_uri)),
					new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

			if (accs != null) {
				return new Json("ok", true);
			}
		}

		return new Json("ok", false);

	}

	public static Json setScopes(String appid, List<String> scopes, Users user) {
		if (scopes != null && Scopes.scopes.containsAll(scopes)) {
			scopes = Scopes.sort(scopes);
			Json accs = Db.findOneAndUpdate("ApiApps", Filters.and(Filters.eq("_id", appid), Filters.eq("user", user.getId())),
					new Json("$set", new Json("scopes", scopes)),
					new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

			if (accs != null) {
				return new Json("ok", true);
			}
		}

		return new Json("ok", false);

	}

	public static Object getAccesses(Users user) {


		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.match(Filters.eq("user", user.getId())));

		pipeline.add(Aggregates.sort(Sorts.descending("date")));
		pipeline.add(Aggregates.lookup("ApiApps", "client_id", "client_id", "app"));
		pipeline.add(Aggregates.unwind("$app"));

		return Db.aggregate("ApiAccess", pipeline);

	}

	public static List<String> parseScope(String scope_str) {
		if (scope_str == null || scope_str.equals("")) {
			return null;
		}
		List<String> scopes = new ArrayList<>();
		for (String scope : scope_str.split("([ ]+)?([,+\\-])([ ]+)?", 60)) {
			if (Scopes.scopes.contains(scope)) {
				scopes.add(scope);
			}
		}
		scopes = Scopes.sort(scopes);
		return scopes;
	}

	public static Json verifyApp(String client_id, String client_secret, String scope) {
		Json rez = new Json();
		Json app = Db.find("ApiApps", Filters.and(
				Filters.eq("client_id", client_id),
				Filters.eq("client_secret", client_secret),
				Filters.eq("scopes", ApiUtils.parseScope(scope)))).first();
		if (app != null) {
			rez.put("date", app.getDate("date"));
			rez.put("name", app.getString("name"));
			rez.put("logo", app.getString("logo"));
		} else {
			rez.put("error", "UNKNOWN_APP");
		}
		return rez;
	}

	public static class Scopes {

		public final static List<String> scopes = new ArrayList<>();

		static {
			scopes.addAll(
					Arrays.asList(
							"email",
							"pm",
							"threads",
							"bills",
							"buy",
							"cash",
							"accounts"
					)
			);


		}

		public static List<String> sort(List<String> scopes) {
			List<String> sorted = new ArrayList<>();
			scopes.forEach(scope -> {
				if (scopes.contains(scope)) {
					sorted.add(scope);
				}
			});
			return sorted;
		}

	}
}
