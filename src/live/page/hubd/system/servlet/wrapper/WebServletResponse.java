/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet.wrapper;

import jakarta.servlet.ServletResponse;
import live.page.hubd.system.Settings;
import live.page.hubd.system.cosmetic.UiStyleServlet;
import live.page.hubd.system.cosmetic.tmpl.BaseTemplate;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.json.XMLJsonParser;
import live.page.hubd.system.sessions.BaseCookie;
import org.apache.http.HttpHeaders;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class WebServletResponse extends BaseServletResponse {

    private final static BaseTemplate template = new BaseTemplate();
    private final long start = System.currentTimeMillis();

    public WebServletResponse(ServletResponse response) throws IOException {
        super(response);
        setHeader("Referrer-Policy", Settings.REFERRER_POLICY);
        setHeader("Vary", "Accept-Language,X-Requested-With,Cookie");
    }

    public void sendText(String msg) throws IOException {
        try {
            setHeader(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8");
        } catch (Exception ignore) {
        }
        getWriter().write(msg);
    }

    public void sendTemplate(WebServletRequest req, String file) throws IOException {

        long xspeed = System.currentTimeMillis() - start;

        if (req.getAttribute("title") != null) {
            setHeader("X-Title", URLEncoder.encode(req.getAttribute("meta_title").toString(), StandardCharsets.UTF_8));
        }

        setCharacterEncoding(StandardCharsets.UTF_8.displayName());

        if (file.endsWith(".xml")) {
            setContentType("application/xml; charset=utf-8");
        } else if (file.endsWith(".txt")) {
            setContentType("text/plain; charset=utf-8");
        } else {
            setContentType("text/html; charset=utf-8");
        }

        if (req.getAttribute("canonical") != null) {
            setHeader("Link", "<" + req.getAttribute("canonical") + ">; rel=canonical");
        }
        if (!req.isAjax()) {
            //addHeader("Link", UiStyleServlet.getPreloadHeader());
        }
        req.setAttribute("csslinks", UiStyleServlet.getCssLinks());

        req.setAttribute("xspeed", xspeed);
        setHeader("X-Speed", xspeed + "ms");

        Map<String, Object> attrs = new HashMap<>();
        Enumeration<String> attrs_name = req.getAttributeNames();
        while (attrs_name.hasMoreElements()) {
            String name = attrs_name.nextElement();
            attrs.put(name, req.getAttribute(name));
        }

        Json reqpar = new Json();
        Enumeration<String> par = req.getParameterNames();
        while (par.hasMoreElements()) {
            String key = par.nextElement();
            reqpar.put(key, req.getParameter(key));
        }
        attrs.put("req", reqpar);
        attrs.put("referer", req.getHeader(HttpHeaders.REFERER));

        attrs.put("consent", BaseCookie.get(req, "consent"));

        template.process(file, attrs, getWriter());
    }

    public void sendJson(Object obj) {
        try {
            setHeader("Content-Type", "application/json; charset=utf-8");
            setHeaderNoCache();
            setHeader("X-Robots-Tag", "noindex");
            setHeader("X-Speed", (System.currentTimeMillis() - start) + "ms");
            getWriter().write(XMLJsonParser.toJSON(obj));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendError(int status, WebServletRequest request) throws IOException {
        setHeaderNoCache();
        setStatus(status);
        request.setAttribute("status", status);
        sendTemplate(request, "/error.html");
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        setHeaderNoCache();
        setStatus(status);
        getWriter().write(message == null ? "Error " + status : message);
    }

    @Override
    public void sendError(int status) throws IOException {
        sendError(status, (String) null);
    }

}
