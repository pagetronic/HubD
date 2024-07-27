/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet.utils;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.utils.Fx;
import org.apache.commons.lang3.StringUtils;

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
        return requestURI.split("\\?")[0].replaceFirst(".*/([^/]+)$", "$1");
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
        return "<meta name=\"robots\" content=\"" + StringUtils.join(Arrays.asList((!index ? "no" : "") + "index", (!follow ? "no" : "") + "follow", "noarchive"), ", ") + "\" />";
    }
}
