/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.profile;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.Date;

public class CookiesUtils {

	/**
	 * log consent (RGPD/GDPR usage)
	 */
	public static Json consent(String id, String ip, boolean accept, String choice) {
		return new Json("ok", Db.save("Consents", new Json("uid", id).put("ip", ip).put("date", new Date()).put("consent", accept).put("type", choice)));
	}

	/**
	 * log adBlock users
	 */
	public static Json adBlock(String ip, boolean accept) {
		Date date = new Date();
		UpdateResult rez = Db.getDb("AdBlock").updateOne(Filters.eq("_id", ip),
				new Json("$set",
						new Json("adblock", accept).put("update", date))
						.put("$setOnInsert",
								new Json("_id", ip).put("date", date)
						), new UpdateOptions().upsert(true)
		);
		return new Json("ok", rez.getMatchedCount() > 0 || rez.getModifiedCount() > 0);
	}

	/**
	 * purge all cookies
	 */
	public static boolean purge(WebServletRequest req, WebServletResponse resp) throws IOException {
		if (req.contains("purge")) {
			WebServletResponse.setHeaderNoCache(resp);
			Cookie[] cookies = req.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals(Settings.getCookieName())) {
						cookie.setHttpOnly(true);
						cookie.setSecure(true);
						cookie.setDomain(Settings.STANDARD_HOST);
					}
					cookie.setMaxAge(0);
					resp.addCookie(cookie);
				}
			}
			resp.sendRedirect("/profile");
			return true;
		}
		return false;
	}
}
