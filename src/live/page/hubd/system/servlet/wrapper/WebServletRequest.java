/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet.wrapper;

import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletRequest;
import live.page.hubd.content.pages.PagesUtils;
import live.page.hubd.system.Language;
import live.page.hubd.system.Settings;
import live.page.hubd.system.cosmetic.UiStyleServlet;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.utils.ServletUtils;
import live.page.hubd.system.sessions.Users;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WebServletRequest extends BaseServletRequest {


    public WebServletRequest(ServletRequest request) throws IOException {
        super(request);


        setCharacterEncoding(StandardCharsets.UTF_8.displayName());


        setRobotsIndex(false);
        setAttribute("ajax", isAjax());

        setAttribute("lng", lng);

        setTitle(Language.exist("SITE_TITLE", getLng()) ? Language.get("SITE_TITLE", getLng()) : Settings.SITE_TITLE);

        setAttribute("jslink", UiStyleServlet.getJsLink());
        setAttribute("csslinks", UiStyleServlet.getCssLinks());
        //setAttribute("css", UiStyleServlet.getCssStaticContent());

        setAttribute("http_site", Settings.getFullHttp(getLng()));

        setAttribute("requesturi", getRequestURI());

        setAttribute("robotsmetas", ServletUtils.setRobotsIndex(false, false));

        String ip = getHeader("X-FORWARDED-FOR");
        if (ip == null) {
            ip = ServletUtils.realIp(this);
        }
        setAttribute("ip", ip);

        if (getMethod().equals("GET")) {
            setCanonical(getRequestURI(), getParameterMap());
        } else {
            setCanonical(getRequestURI());
        }

    }

    public void setTitle(String title) {
        if (title != null) {
            setAttribute("title", title);
            setMetaTitle(title);
        } else {
            setAttribute("title", null);
        }
    }

    public void setMetaTitle(String meta_title) {
        String site_title = Language.exist("SITE_TITLE", getLng()) ? Language.get("SITE_TITLE", getLng()) : Settings.SITE_TITLE;
        if (meta_title != null) {
            setAttribute("meta_title", meta_title);
        } else {
            setAttribute("meta_title", site_title);
        }
    }

    public void setDescription(String description) {
        if (description != null) {
            setAttribute("description", description.replace("\\\"", "\"").replace("\"", "&#34;"));
        } else {
            setAttribute("description", null);
        }
    }

    public void setCanonical(String canonical, Map<String, String[]> params) {
        List<String> params_ = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            for (String value : entry.getValue()) {
                params_.add(entry.getKey() + (value.isEmpty() ? "" : "=" + value));
            }
        }
        setCanonical(canonical + (!params.isEmpty() ? "?" + StringUtils.join(params_, "&") : ""));
    }

    public void setCanonical(String canonical, String... keys) {
        List<String> params = new ArrayList<>();
        Map<String, String[]> map = getParameterMap();
        for (String key : keys) {
            if (map.get(key) != null) {
                for (String param : map.get(key)) {
                    params.add(key + "=" + param);
                }
            }
        }
        setCanonical(canonical, params);
    }

    public void setCanonical(String canonical, List<String> params) {
        setCanonical(canonical + (!params.isEmpty() ? "?" + StringUtils.join(params, "&") : ""));
    }

    public void setCanonical(String canonical) {
        if (canonical == null || !Settings.domainAvailable(getServerName())) {
            removeAttribute("canonical");
            return;
        }
        setAttribute("canonical", Settings.getFullHttp(getLng()) + canonical);
        setAttribute("base_canonical", canonical);
    }

    public void setImageOg(String image) {
        setAttribute("og_image", image);
    }

    @Override
    public String getRequestURI() {
        if (getAttribute("requestURI") != null) {
            return getAttribute("requestURI").toString();
        } else {
            return super.getRequestURI();
        }
    }

    public int getInteger(String key, int def) {
        try {
            return Integer.parseInt(getParameter(key));
        } catch (Exception e) {
            return def;
        }
    }

    public double getDouble(String key, double def) {
        try {
            return Double.parseDouble(getParameter(key));
        } catch (Exception e) {
            return def;
        }
    }

    public String getString(String key, String def) {
        if (getParameter(key) != null) {
            try {
                return URLDecoder.decode(getParameter(key), StandardCharsets.UTF_8);
            } catch (Exception e) {
                return getParameter(key);
            }
        }
        return def;
    }

    public void setUser(Users user) {

        setAttribute("user", user);
        if (user != null && !user.getString("lng", "").equals(lng)) {
            Db.updateOne("Users", Filters.eq("_id", user.getId()), new Json("$set", new Json("lng", lng)));
        }

        if (user != null && user.getAdmin()) {
            setAttribute("draft_count", PagesUtils.getDraftsCount());
        }
        AddToServletRequest.seed(user, this);

    }


    public boolean isAjax() {
        return (getHeader("X-Requested-With") != null && getHeader("X-Requested-With").equalsIgnoreCase("xmlhttprequest"));
    }

    @Override
    public String getServerName() {
        if (super.getServerName() == null) {
            return "127.0.0.1";
        }
        return super.getServerName();
    }

    public void setBreadCrumb(List<Json> breadcrumb) {
        setAttribute("breadcrumb", breadcrumb);
    }


    @SuppressWarnings("unchecked")
    public void addBreadCrumb(String title, String url) {
        List<Json> breadcrumb = (List<Json>) getAttribute("breadcrumb");
        if (breadcrumb == null) {
            breadcrumb = new ArrayList<>();
        }
        breadcrumb.add(new Json("title", title).put("url", url));
        setBreadCrumb(breadcrumb);
    }

    public void setBreadCrumbTitle(String title) {
        setAttribute("breadcrumb_title", title);
    }

    public String getReferer(String def) {
        String referer = getHeader(HttpHeaders.REFERER);
        return referer != null && referer.startsWith("http") ? referer : def;
    }

    public void setRobotsIndex(boolean index) {
        if (index) {
            setAttribute("robotsmetas", ServletUtils.setRobotsIndex(true, true));
        } else {
            setAttribute("robotsmetas", ServletUtils.setRobotsIndex(false, false));
        }
    }

    public void setRobotsIndex(boolean index, boolean follow) {
        setAttribute("robotsmetas", ServletUtils.setRobotsIndex(index, follow));
    }

}
