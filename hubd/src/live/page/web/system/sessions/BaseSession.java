/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.sessions;

import com.mongodb.client.model.*;
import live.page.web.content.posts.utils.DiscussAdmin;
import live.page.web.content.users.UsersUtils;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.utils.BruteLocker;
import live.page.web.system.servlet.utils.ServletUtils;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.utils.Fx;
import live.page.web.utils.Mailer;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@WebListener
public class BaseSession implements ServletContextListener {

	private static final ExecutorService service = Executors.newFixedThreadPool(10);

	public static Json createSession(HttpServletRequest req, HttpServletResponse resp) {
		Json session = new Json();

		session.put("expire", new Date(System.currentTimeMillis() + (Settings.COOKIE_DELAY * 1000L)));
		session.put("_id", Fx.getSecureKey());
		session.put("ip", ServletUtils.realIp(req));
		session.put("ua", req.getHeader("User-Agent"));
		Db.getDb("Sessions").insertOne(session);
		BaseCookie gaia = new BaseCookie(session.getId());
		resp.addHeader("Set-Cookie", gaia.stringHeader());
		return session;
	}

	private static String createSession(HttpServletRequest req, HttpServletResponse resp, String user_id) {
		Json session = getOrCreateSession(req, resp);
		session.put("user", user_id);
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				if (cookies[i].getName().equals("sysid")) {

					DiscussAdmin.updateSysId(cookies[i].getValue(), user_id);

					cookies[i].setMaxAge(0);
					resp.addCookie(cookies[i]);
					break;
				}
			}
		}
		Db.save("Sessions", session);
		return session.getId();
	}

	public static Users getOrCreateUser(ServletRequest request, ServletResponse response) {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;


		BaseCookie gaia = BaseCookie.getAuth(req);
		if (gaia == null) {
			return null;
		}
		gaia.setMaxAge(Settings.COOKIE_DELAY);
		//strange, have to clone
		resp.addCookie((BaseCookie) gaia.clone());

		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.match(Filters.eq("_id", gaia.getValue())));
		pipeline.add(Aggregates.lookup("Users", "user", "_id", "user"));
		pipeline.add(Aggregates.unwind("$user", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.limit(1));
		pipeline.add(new Json("$addFields", new Json("user.original", "$original").put("user.provider", "$provider")));
		pipeline.add(Aggregates.replaceRoot("$user"));

		pipeline.add(Aggregates.lookup("Teams", "teams", "_id", "teams"));
		pipeline.add(Aggregates.lookup("Users", "original", "_id", "original"));

		pipeline.add(Aggregates.lookup("Users", "_id", "parent", "childrens"));


		pipeline.add(new Json("$addFields", new Json()
				.put("childrens", new Json("$size", "$childrens"))

				.put("url", new Json("$concat", Arrays.asList("/users/", "$_id")))
				.put("coins", "$coins")
				.put("avatar",
						new Json("$concat", Arrays.asList(
								Settings.getCDNHttp(),
								new Json("$cond",
										Arrays.asList(new Json("$eq", Arrays.asList("$avatar", new BsonUndefined())),
												Settings.UI_LOGO,
												new Json("$concat", Arrays.asList("/files/", "$avatar"))))
						))
				)
				.put("original", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$size", "$original"), 0)), null, "$original"))))
		);

		Json user = Db.aggregate("Sessions", pipeline).first();


		if (user != null && user.getId() != null) {
			service.submit(() -> {
				Db.updateOne("Users", Filters.eq("_id", user.getId()), new Json("$set", new Json("last", new Date())));
			});
			return new Users(user);
		} else {
			if (user != null && !user.containsKey("provider")) {
				BaseCookie.clearAuth(req, resp);
			}
			return null;
		}
	}

	public static boolean sessionExists(HttpServletRequest req) {
		BaseCookie gaia = BaseCookie.getAuth(req);
		return gaia != null && !gaia.getValue().equals("") && Db.exists("Sessions", Filters.eq("_id", gaia.getValue()));
	}


	public static void deleteUserSessions(String user_id) {
		Db.deleteMany("Sessions", Filters.eq("user", user_id));
	}

	public static void removeAllCookies(HttpServletRequest req, HttpServletResponse resp) {
		final Cookie[] cookies = req.getCookies();
		if ((cookies != null) && (cookies.length > 0)) {
			for (Cookie cooky : cookies) {
				cooky.setMaxAge(0);
				resp.addCookie(cooky);
			}
		}

	}

	public static Json getOrCreateSession(HttpServletRequest req, HttpServletResponse resp) {
		Json session = getSession(req);
		if (session == null) {
			return createSession(req, resp);
		}
		return session;
	}

	public static Json getSession(HttpServletRequest req) {
		BaseCookie gaia = BaseCookie.getAuth(req);
		return (gaia == null) ? null : Db.findById("Sessions", gaia.getValue());
	}

	public static Json register(HttpServletRequest req, HttpServletResponse resp, String name, String email, String new_password, Json settings, String key) {
		Json res = new Json();

		Json errors = new Json();

		if (email.equals("")) {
			errors.add("email", "EMPTY");
		} else if (!email.matches("^\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")) {
			errors.add("email", "INCONSISTENT");
		} else if (Db.exists("Users", Filters.eq("email", email))) {
			errors.add("email", "EXIST");
		} else if (email.length() > 50) {
			errors.add("email", "TOO_LONG");
		}
		if (new_password.length() < 5) {
			errors.add("new-password", "TOO_SHORT");
		} else if (new_password.length() > 200) {
			errors.add("new-password", "TOO_LONG");
		}
		if (key == null) {
			if (name.length() < 4) {
				errors.add("name", "TOO_SHORT");
			} else if (name.length() > 50) {
				errors.add("name", "TOO_LONG");
			} else {
				name = UsersUtils.uniqueName(name);
			}
		}

		if (!errors.isEmpty()) {

			res.put("errors", errors);
			return res;
		}

		Json user;
		if (key != null && !key.equals("")) {

			user = Db.findOneAndUpdate("Users", Filters.eq("key", key),
					new Json()
							.put("$unset", new Json("key", ""))
							.put("$set", new Json().put("email", email).put("password", Fx.crypt(new_password)))
					, new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

		} else {

			user = UsersBase.getBase();
			user.put("name", name);
			user.put("email", email);
			user.put("settings", settings);
			user.put("password", Fx.crypt(new_password));
			Db.save("Users", user);
		}
		if (user == null) {
			return res.put("ok", false);
		}
		createSession(req, resp, user.getId());

		res = getUserData(user, false);
		return res.put("ok", true).put("user", user.getId());

	}

	public static Json login(HttpServletRequest req, HttpServletResponse resp, String email, String password) {
		Json res = new Json("ok", false);
		if (BruteLocker.isBan(ServletUtils.realIp(req))) {
			return res;
		}
		Json user = Db.find("Users",
				Filters.and(
						Filters.eq("email", email),
						Filters.eq("password", Fx.crypt(password))
				)
		).first();
		if (user != null) {
			res = getUserData(user, false);
			createSession(req, resp, user.getId());
			res.put("ok", true);
		}
		return res;
	}

	public static Json recover(ApiServletRequest req, String email) {
		Json res = new Json("ok", false);
		if (BruteLocker.isBan(ServletUtils.realIp(req))) {
			return res;
		}
		Json user = Db.findOneAndUpdate("Users",
				Filters.eq("email", email),
				new Json("$set", new Json("activate", Fx.getSecureKey())),
				new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
		);
		if (user != null) {
			res = new Json("ok", Mailer.sendActivation(req.getLng(), user.getString("email"), user.getString("activate")));
		}
		return res;
	}

	public static Users activate(WebServletRequest req, WebServletResponse resp, String activate) {
		Json user = Db.findOneAndUpdate("Users",
				Filters.eq("activate", activate),
				new Json("$set", new Json("activate", Fx.getSecureKey())),
				new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
		);
		if (user == null) {
			return null;
		}
		createSession(req, resp, user.getId());
		return new Users(user);
	}

	public static Json password(String email, String password, String activate) {

		if (password.length() < 5) {
			return new Json("ok", false).put("password", "TOO_SHORT");
		}

		return new Json("ok", Db.updateOne("Users",
				Filters.and(Filters.eq("email", email), Filters.eq("activate", activate)),
				new Json("$unset", new Json("activate", "")).put("$set", new Json("password", Fx.crypt(password)))
		).getModifiedCount() > 0);
	}

	public static Json avatar(Users user, String avatar) {
		if (avatar == null || !Db.exists("BlobFiles", Filters.eq("_id", avatar))) {
			return new Json("ok", false);
		}
		return new Json("ok", Db.updateOne("Users",
				Filters.eq("_id", user.getId()),
				new Json("$set", new Json("avatar", avatar))
		).getModifiedCount() > 0);
	}

	public static Json getUserData(Json user_, boolean email) {
		Users user = new Users(user_);
		Json data = new Json();

		data.put("id", user.getId());
		data.put("name", user.getString("name"));
		if (email) {
			data.put("email", user.getString("email"));
		}
		data.put("join", user.getDate("join"));
		data.put("unreads", user.getInteger("unreads", 0));
		data.put("notices", user.getInteger("notices", 0));
		data.put("cash", user.getJson("cash"));
		data.put("coins", user.getInteger("coins", 0));

		Json settings = user.getJson("settings");
		if (settings != null) {
			data.put("currency", settings.getString("currency"));
		}


		if (user.getString("avatar") != null) {
			data.put("logo", (user.getString("avatar").startsWith("http") ? "" : Settings.getCDNHttp() + "/files/") + user.getString("avatar"));
		} else {
			data.put("logo", Settings.getLogo());
		}
		if (user.getAdmin()) {
			data.put("admin", true);
		}
		if (user.getEditor()) {
			data.put("editor", true);
		}
		return data;
	}

	public static void remove(WebServletRequest req, WebServletResponse resp) {
		BaseCookie cookie = BaseCookie.getAuth(req);
		if (cookie != null) {
			cookie.setMaxAge(0);
			resp.addCookie(cookie);
		}
	}

	public static Json tos(Users user, boolean accept) {
		if (accept) {
			Db.updateOne("Users", Filters.eq("_id", user.getId()), new Json("$set", new Json("tos", new Date())));
		} else {
			Db.updateOne("Users", Filters.eq("_id", user.getId()), new Json("$unset", new Json("tos", "")));
		}
		return new Json("ok", true);
	}


	@Override
	public void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Fx.shutdownService(service);
	}
}
