/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.servlet.utils;

import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.utils.Fx;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

public class ServletUtils {

	/**
	 * Get Real IP with proxy identification
	 */
	public static String realIp(ServletRequest req) {
		String ip = ((HttpServletRequest) req).getHeader("X-FORWARDED-FOR");
		if (ip == null) {
			ip = req.getRemoteAddr();
		} else {
			ip += "@" + req.getRemoteAddr();
		}
		return ip;
	}

	public static String parseId(String requestURI) {
		return (requestURI.matches(".*/([A-Z0-9]{" + Db.DB_KEY_LENGTH + ",}).*")) ? requestURI.replaceFirst(".*/([A-Z0-9]{" + Db.DB_KEY_LENGTH + ",}).*", "$1") : null;
	}

	public static void redirect301(String location, ServletResponse resp) {
		if (location == null) {
			Fx.log("no location for redirect 301");
			return;
		}
		HttpServletResponse resp_ = (HttpServletResponse) resp;
		resp_.setStatus(301);
		if (!Fx.IS_DEBUG) {
			WebServletResponse.setHeaderMaxCache(resp_);
		} else {
			WebServletResponse.setHeaderNoCache(resp_);
		}
		resp_.setHeader("Connection", "close");
		resp_.setHeader("Location", location);

	}

	public static String setRobotsIndex(boolean index, boolean follow) {

		String meta = "<meta name=\"robots\" content=\"" + StringUtils.join(Arrays.asList((!index ? "no" : "") + "index", (!follow ? "no" : "") + "follow", "noarchive"), ", ") + "\" />";
		if (!index && Settings.PUBS) {
			meta += "\n<meta name=\"mediapartners-google\" content=\"index\" />";
		}
		return meta;

	}
}
