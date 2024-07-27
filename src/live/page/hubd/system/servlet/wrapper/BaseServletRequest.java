/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet.wrapper;

import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.utils.ServletUtils;
import live.page.hubd.system.sessions.AuthType;
import live.page.hubd.system.sessions.BaseCookie;
import org.apache.http.HttpHeaders;

import java.util.Enumeration;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Base wrapper for Servlet Requests
 */
public class BaseServletRequest extends HttpServletRequestWrapper {

    public String lng = "fr";
    public AuthType authType;

    public BaseServletRequest(ServletRequest request) {
        super((HttpServletRequest) request);
        if (Settings.domainAvailable(getServerName())) {
            lng = Settings.getLang(getServerName());
        }

        String tz = BaseCookie.get(this, "tz");
        try {
            setAttribute("tz", tz != null ? Integer.valueOf(tz) : 0);
        } catch (Exception ignore) {

        }
    }

    /**
     * Try to get the ID
     */
    public String getId() {
        return getId(null);
    }

    /**
     * Try to get the ID
     */
    public String getId(String escape) {
        if (getParameter("id") != null) {
            return getParameter("id");
        }
        String id = ServletUtils.parseId(escape == null ? getRequestURI() : getRequestURI()
                .replaceAll(Pattern.compile(Pattern.quote(escape) + "$").pattern(), "")
                .replaceAll("/$", ""));
        return id.isEmpty() ? null : id;
    }

    /**
     * Get the correct request URI after @WebFilter modification
     */
    @Override
    public String getRequestURI() {
        if (getAttribute("requestURI") != null) {
            return getAttribute("requestURI").toString();
        }
        return super.getRequestURI();
    }

    /**
     * Parse parameter as Integer
     */
    public int getInteger(String key, int def) {
        try {
            return Integer.valueOf(getParameter(key));
        } catch (Exception e) {
            return def;
        }
    }


    /**
     * Parse parameter as Double = dotted number
     */
    public double getDouble(String key, double def) {
        try {
            return Double.valueOf(getParameter(key));
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Parse parameter as String
     */
    public String getString(String key, String def) {
        if (getParameter(key) != null) {
            return getParameter(key);
        }
        return def;

    }

    /**
     * Get lang detected
     */
    public String getLng() {
        return lng;
    }


    /**
     * Test if request contains parameter
     */
    public boolean contains(String key) {
        Enumeration<String> attr = super.getParameterNames();
        while (attr.hasMoreElements()) {
            if (attr.nextElement().equals(key)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Get Real IP with proxy identification
     */
    public String getRealIp() {
        return ServletUtils.realIp(this);
    }


    /**
     * Get Data saved in session
     */
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


    /**
     * Save Data in session
     */
    public void setSessionData(Json data) {
        BaseCookie cookie = BaseCookie.getAuth(this);
        if (cookie == null) {
            return;
        }
        Db.updateOne("Sessions", Filters.eq("_id", cookie.getValue()), new Json("$set", new Json("data", data)));
    }


    /**
     * Get User-Agent string
     */
    public String getUserAgent() {
        return getHeader(HttpHeaders.USER_AGENT);
    }

    public TimeZone getTz() {
        String tz = BaseCookie.get(this, "tz");
        if (getParameter("tz") != null) {
            tz = getParameter("tz");
        }
        try {
            return TimeZone.getTimeZone(TimeZone.getAvailableIDs(-(Integer.parseInt(tz) * 60 * 1000))[0]);
        } catch (Exception ignore) {
        }
        return TimeZone.getTimeZone("UTC");
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }
}
