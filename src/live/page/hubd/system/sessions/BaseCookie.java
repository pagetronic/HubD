/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.sessions;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import live.page.hubd.system.Settings;

import java.io.Serial;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class BaseCookie extends Cookie {

    @Serial
    private final static long serialVersionUID = 3328634754185968841L;


    public BaseCookie(String value) {
        super(Settings.getCookieName(), value);
        setPath("/");
        setDomain(Settings.STANDARD_HOST);
        setSecure(true);
        setHttpOnly(true);
        setMaxAge(Settings.COOKIE_DELAY);
        setAttribute("sameSite", "None");

    }

    public BaseCookie(Cookie cookie) {
        this(cookie.getValue());
    }

    public static BaseCookie getAuth(HttpServletRequest req) {

        try {
            Cookie[] cookies = req.getCookies();
            if (cookies == null) {
                return null;
            }
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(Settings.getCookieName()) && !cookie.getValue().isEmpty()) {
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
                resp.setHeader("Set-Cookie", cookie.stringHeader());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String get(ServletRequest request, String name) {
        HttpServletRequest req = (HttpServletRequest) request;
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name) && !cookie.getValue().isEmpty()) {
                return URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    public String stringHeader() {
        StringBuilder s = new StringBuilder();
        s.append(getName()).append("=").append(getValue());
        if (getMaxAge() >= 0) {
            s.append("; Max-Age=").append(getMaxAge());
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
        if (getAttribute("SameSite") != null) {
            s.append("; SameSite=").append(getAttribute("SameSite"));
        }

        return s.toString();
    }

}