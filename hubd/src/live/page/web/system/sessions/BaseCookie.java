/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.sessions;

import live.page.web.system.Settings;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class BaseCookie extends Cookie {

	private final static long serialVersionUID = 3328634754185968841L;

	private String sameSite = null;

	public BaseCookie(String value) {
		super(Settings.getCookieName(), value);
		setPath("/");
		setDomain(Settings.STANDARD_HOST);
		setSecure(true);
		setHttpOnly(true);
		setMaxAge(Settings.COOKIE_DELAY);
		sameSite = "None";

	}

	public BaseCookie(String key, String value) {
		super(key, value);
		setPath("/");
		setDomain(Settings.STANDARD_HOST);
		setMaxAge(Settings.COOKIE_DELAY);
	}


	public BaseCookie(Cookie cookie) {
		this(cookie.getValue());
		setHttpOnly(cookie.isHttpOnly());
	}

	public static BaseCookie getAuth(HttpServletRequest req) {

		try {
			Cookie[] cookies = req.getCookies();
			if (cookies == null) {
				return null;
			}
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(Settings.getCookieName()) && !cookie.getValue().equals("")) {
					return new BaseCookie(cookie);
				}
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	public static void clearAuth(HttpServletRequest req, HttpServletResponse resp) {
		try {
			BaseCookie cookie = getAuth(req);
			if (cookie != null) {
				cookie.setMaxAge(0);
				resp.addCookie(cookie);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void set(HttpServletResponse resp, String name, String value) {
		BaseCookie cookie = new BaseCookie(name, value);
		try {
			resp.addCookie(cookie);
		} catch (Exception e) {
		}
	}

	public static String get(ServletRequest request, String name) {
		HttpServletRequest req = (HttpServletRequest) request;
		Cookie[] cookies = req.getCookies();
		if (cookies == null) {
			return null;
		}
		for (int i = 0; i < cookies.length; i++) {
			if (cookies[i].getName().equals(name) && !cookies[i].getValue().equals("")) {
				return URLDecoder.decode(cookies[i].getValue(), StandardCharsets.UTF_8);
			}
		}
		return null;
	}


	public String stringHeader() {
		StringBuilder s = new StringBuilder();
		s.append(getName() + "=" + getValue());
		if (getMaxAge() >= 0) {
			s.append("; Max-Age=" + getMaxAge());
		}
		if (getDomain() != null) {
			s.append("; Domain=").append(getDomain());
		}
		if (getPath() != null) {
			s.append("; Path=").append(getPath());
		}
		if (getSecure()) {
			s.append("; Secure");
		}
		if (isHttpOnly()) {
			s.append("; HttpOnly");
		}
		if (sameSite != null) {
			s.append("; SameSite=" + sameSite);
		}

		return s.toString();
	}

}