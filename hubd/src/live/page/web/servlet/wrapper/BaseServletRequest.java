/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.servlet.wrapper;

import com.mongodb.client.model.Filters;
import live.page.web.db.Db;
import live.page.web.servlet.utils.ServletUtils;
import live.page.web.session.BaseCookie;
import live.page.web.utils.Settings;
import live.page.web.utils.json.Json;
import org.apache.http.HttpHeaders;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Enumeration;

public class BaseServletRequest extends HttpServletRequestWrapper {

	public String lng = "fr";

	public BaseServletRequest(ServletRequest request) {
		super((HttpServletRequest) request);
		if (Settings.LANGS_DOMAINS.containsValue(getServerName())) {
			lng = Settings.getLang(getServerName());
		}

		String tz = BaseCookie.get(this, "tz");
		setAttribute("tz", tz != null ? Integer.valueOf(tz) : 0);
		setAttribute("pub", Settings.PUBS);
	}


	public String getId() {
		if (getAttribute("_id") != null) {
			return getAttribute("_id").toString();
		}
		if (getParameter("id") != null) {
			return getParameter("id");
		}
		return ServletUtils.parseId(getRequestURI());

	}

	@Override
	public String getRequestURI() {
		if (getAttribute("requestURI") != null) {
			return getAttribute("requestURI").toString();
		}
		return super.getRequestURI();
	}

	public int getInteger(String key, int def) {
		try {
			return Integer.valueOf(getParameter(key));
		} catch (Exception e) {
			return def;
		}
	}


	public double getDouble(String key, double def) {
		try {
			return Double.valueOf(getParameter(key));
		} catch (Exception e) {
			return def;
		}
	}

	public String getString(String key, String def) {
		if (getParameter(key) != null) {
			return getParameter(key);
		}
		return def;

	}

	public String getLng() {
		return lng;
	}


	public boolean contains(String key) {
		Enumeration<String> attr = super.getParameterNames();
		while (attr.hasMoreElements()) {
			if (attr.nextElement().equals(key)) {
				return true;
			}
		}
		return false;
	}


	public String getRealIp() {
		return ServletUtils.realIp(this);
	}

	public Json getSessionData() {
		BaseCookie cookie = BaseCookie.getAuth(this);
		if (cookie == null) {
			return new Json();
		}
		Json session = Db.findById("Sessions", cookie.getValue());
		if (session == null) {
			return new Json();
		}
		Json data = session.getJson("data");
		if (data == null) {
			return new Json();
		}
		return session.getJson("data");

	}


	public void setSessionData(Json data) {
		BaseCookie cookie = BaseCookie.getAuth(this);
		if (cookie == null) {
			return;
		}
		Db.updateOne("Sessions", Filters.eq("_id", cookie.getValue()), new Json("$set", new Json("data", data)));
	}

	public String getUserAgent() {
		return getHeader(HttpHeaders.USER_AGENT);
	}
}
